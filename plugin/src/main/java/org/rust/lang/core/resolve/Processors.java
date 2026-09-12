/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import consulo.language.editor.completion.CompletionResultSet;
import consulo.util.collection.SmartList;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.cargo.toolchain.RustChannel;
import org.rust.lang.core.completion.RsCompletionContext;
import org.rust.lang.core.completion.CompletionUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve.ref.MethodResolveVariant;
import org.rust.lang.core.resolve2.RsModInfo;
import org.rust.lang.core.types.BoundElement;
import org.rust.lang.core.types.Substitution;
import org.rust.lang.core.types.SubstitutionUtil;
import org.rust.lang.core.types.infer.TypeFolder;
import org.rust.lang.core.types.infer.FoldUtil;
import org.rust.lang.core.types.ty.*;
import org.rust.lang.core.psi.RsCodeFragment;
import org.rust.lang.core.psi.RsStability;
import org.rust.lang.core.psi.RsImplItem;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Contains processor interfaces and utility functions for name resolution.
 */
public final class Processors {
    private Processors() {}

    // --- collectResolveVariants ---

    @Nonnull
    public static List<RsElement> collectResolveVariants(@Nullable String referenceName, @Nonnull Consumer<RsResolveProcessor> f) {
        if (referenceName == null) return Collections.emptyList();
        ResolveVariantsCollector processor = new ResolveVariantsCollector(referenceName);
        f.accept(processor);
        return processor.result;
    }

    @Nonnull
    public static <T extends ScopeEntry> List<T> collectResolveVariantsAsScopeEntries(
        @Nullable String referenceName,
        @Nonnull Consumer<RsResolveProcessorBase<T>> f
    ) {
        if (referenceName == null) return Collections.emptyList();
        ResolveVariantsAsScopeEntriesCollector<T> processor = new ResolveVariantsAsScopeEntriesCollector<>(referenceName);
        f.accept(processor);
        return processor.result;
    }

    @Nonnull
    public static List<RsPathResolveResult<RsElement>> collectPathResolveVariants(
        @Nonnull PathResolutionContext ctx,
        @Nonnull RsPath path,
        @Nonnull Consumer<RsResolveProcessor> f
    ) {
        String referenceName = path.getReferenceName();
        if (referenceName == null) return Collections.emptyList();
        SinglePathResolveVariantsCollector processor = new SinglePathResolveVariantsCollector(ctx, referenceName);
        f.accept(processor);
        return processor.result;
    }

    @Nonnull
    public static Map<RsPath, List<RsPathResolveResult<RsElement>>> collectMultiplePathResolveVariants(
        @Nonnull PathResolutionContext ctx,
        @Nonnull List<RsPath> paths,
        @Nonnull Consumer<RsResolveProcessor> f
    ) {
        Map<RsPath, List<RsPathResolveResult<RsElement>>> result = new HashMap<>();
        Map<String, List<RsPathResolveResult<RsElement>>> resultByName = new HashMap<>();
        for (RsPath path : paths) {
            String name = path.getReferenceName();
            if (name == null) continue;
            List<RsPathResolveResult<RsElement>> list = resultByName.computeIfAbsent(name, k -> new SmartList<>());
            result.put(path, list);
        }
        MultiplePathsResolveVariantsCollector processor = new MultiplePathsResolveVariantsCollector(ctx, resultByName);
        f.accept(processor);
        return result;
    }

    @Nullable
    public static RsElement pickFirstResolveVariant(@Nullable String referenceName, @Nonnull Consumer<RsResolveProcessor> f) {
        ScopeEntry entry = pickFirstResolveEntry(referenceName, f);
        return entry != null ? entry.getElement() : null;
    }

    @Nullable
    public static ScopeEntry pickFirstResolveEntry(@Nullable String referenceName, @Nonnull Consumer<RsResolveProcessor> f) {
        if (referenceName == null) return null;
        PickFirstScopeEntryCollector processor = new PickFirstScopeEntryCollector(referenceName);
        f.accept(processor);
        return processor.result;
    }

    @Nonnull
    public static Set<String> collectNames(@Nonnull Consumer<RsResolveProcessor> f) {
        NamesCollector processor = new NamesCollector();
        f.accept(processor);
        return processor.result;
    }

    public static void collectCompletionVariants(
        @Nonnull CompletionResultSet result,
        @Nonnull RsCompletionContext context,
        @Nonnull Consumer<RsResolveProcessor> f
    ) {
        CompletionVariantsCollector processor = new CompletionVariantsCollector(result, context);
        f.accept(processor);
    }

    // --- Processor utility methods ---

    public static boolean processEntry(@Nonnull RsResolveProcessor processor, @Nonnull String name,
                                       @Nonnull Set<Namespace> namespaces, @Nonnull RsElement e) {
        return processor.process(new SimpleScopeEntry(name, e, namespaces, SubstitutionUtil.EMPTY));
    }

    public static boolean processEntry(@Nonnull RsResolveProcessor processor, @Nonnull String name,
                                       @Nonnull RsElement e, @Nonnull Set<Namespace> namespaces,
                                       @Nonnull VisibilityFilter visibilityFilter) {
        return processor.process(new ScopeEntryWithVisibility(name, e, namespaces, visibilityFilter, SubstitutionUtil.EMPTY));
    }

    public static boolean processNamedElement(@Nonnull RsResolveProcessor processor, @Nonnull RsNamedElement e,
                                              @Nonnull Set<Namespace> namespaces) {
        String name = e.getName();
        if (name == null) return false;
        return processEntry(processor, name, namespaces, e);
    }

    public static boolean processAll(@Nonnull RsResolveProcessor processor, @Nonnull List<? extends RsNamedElement> elements,
                                     @Nonnull Set<Namespace> namespaces) {
        for (RsNamedElement element : elements) {
            if (processNamedElement(processor, element, namespaces)) return true;
        }
        return false;
    }

    public static boolean processAllScopeEntries(@Nonnull List<ScopeEntry> elements, @Nonnull RsResolveProcessor processor) {
        for (ScopeEntry entry : elements) {
            if (processor.process(entry)) return true;
        }
        return false;
    }

    public static boolean processAllWithSubst(
        @Nonnull Collection<? extends RsNamedElement> elements,
        @Nonnull Substitution subst,
        @Nonnull Set<Namespace> namespaces,
        @Nonnull RsResolveProcessor processor
    ) {
        for (RsNamedElement e : elements) {
            String name = e.getName();
            if (name == null) continue;
            if (processor.process(new SimpleScopeEntry(name, e, namespaces, subst))) return true;
        }
        return false;
    }

    // --- Filter processors ---

    @SuppressWarnings("unchecked")
    @Nonnull
    public static RsResolveProcessor filterNotCfgDisabledItemsAndTestFunctions(@Nonnull RsResolveProcessor processor) {
        return asResolveProcessor(wrapWithFilter(processor, e -> {
            RsElement element = e.getElement();
            if (element instanceof RsFunction && RsFunctionUtil.isTest((RsFunction) element)) return false;
            if (element instanceof RsDocAndAttributeOwner && !RsDocAndAttributeOwnerUtil.existsAfterExpansionSelf((RsDocAndAttributeOwner) element))
                return false;
            return true;
        }));
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    public static RsResolveProcessor filterCompletionVariantsByVisibility(@Nonnull RsElement context,
                                                                          @Nonnull RsResolveProcessor processor) {
        if (context.getContainingFile() instanceof RsCodeFragment) {
            return processor;
        }
        RsMod contextMod = context.getContainingMod();
        return asResolveProcessor(wrapWithFilter(processor, it -> {
            RsElement element = it.getElement();
            if (element instanceof RsVisible && !RsVisibilityUtil.isVisibleFrom((RsVisible) element, contextMod)) return false;
            if (!isVisibleFrom(it, context)) return false;
            if (element instanceof RsOuterAttributeOwner) {
                if (shouldHideElementInCompletion((RsOuterAttributeOwner) element, context, contextMod)) return false;
            }
            return true;
        }));
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    public static RsResolveProcessor filterNotAttributeAndDeriveProcMacros(@Nonnull RsResolveProcessor processor) {
        return asResolveProcessor(wrapWithFilter(processor, e -> {
            RsElement element = e.getElement();
            if (element instanceof RsFunction) {
                RsFunction fn = (RsFunction) element;
                if (RsFunctionUtil.isProcMacroDef(fn) && !RsFunctionUtil.isBangProcMacroDef(fn)) return false;
            }
            return true;
        }));
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    public static RsResolveProcessor filterAttributeProcMacros(@Nonnull RsResolveProcessor processor) {
        return asResolveProcessor(wrapWithFilter(processor, e -> {
            RsElement element = e.getElement();
            if (!(element instanceof RsFunction)) return false;
            return RsFunctionUtil.isAttributeProcMacroDef((RsFunction) element);
        }));
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    public static RsResolveProcessor filterDeriveProcMacros(@Nonnull RsResolveProcessor processor) {
        return asResolveProcessor(wrapWithFilter(processor, e -> {
            RsElement element = e.getElement();
            if (!(element instanceof RsFunction)) return false;
            return RsFunctionUtil.isCustomDeriveProcMacroDef((RsFunction) element);
        }));
    }

    // --- Visibility utilities ---

    @Nonnull
    public static VisibilityStatus getVisibilityStatusFrom(@Nonnull ScopeEntry entry, @Nonnull RsElement context,
                                                            @Nullable Object lazyModInfo) {
        if (entry instanceof ScopeEntryWithVisibility) {
            return ((ScopeEntryWithVisibility) entry).getVisibilityFilter().apply(context, lazyModInfo);
        }
        return VisibilityStatus.Visible;
    }

    public static boolean isVisibleFrom(@Nonnull ScopeEntry entry, @Nonnull RsElement context) {
        return getVisibilityStatusFrom(entry, context, null) == VisibilityStatus.Visible;
    }

    public static boolean shouldHideElementInCompletion(@Nonnull RsOuterAttributeOwner element,
                                                         @Nonnull RsElement context, @Nonnull RsMod contextMod) {
        RsMod elementContainingMod = element.getContainingMod();
        if (elementContainingMod == contextMod) return false;

        if (RsDocAndAttributeOwnerUtil.getQueryAttributes(element).isDocHidden()) {
            boolean isDeriveMacroInImport = RsPsiJavaUtil.ancestorStrict(context, RsUseItem.class) != null
                && element instanceof RsFunction && RsFunctionUtil.isCustomDeriveProcMacroDef((RsFunction) element)
                && element.getContainingCrate() != contextMod.getContainingCrate();
            if (!isDeriveMacroInImport) return true;
        }

        // Check stability
        var cargoProject = RsElementExtUtil.getCargoProject(contextMod);
        if (cargoProject == null) return false;
        var rustcInfo = cargoProject.getRustcInfo();
        if (rustcInfo == null) return false;
        var version = rustcInfo.getVersion();
        if (version == null) return false;
        RustChannel rustcChannel = version.getChannel();
        boolean showUnstableItems = rustcChannel != RustChannel.STABLE && rustcChannel != RustChannel.BETA;
        if (showUnstableItems) return false;
        if (element.getContainingCrate().getOrigin() != PackageOrigin.STDLIB) return false;

        return getStability(element) != RsStability.Stable;
    }

    @Nonnull
    private static RsStability getStability(@Nonnull RsOuterAttributeOwner element) {
        RsStability stability = RsDocAndAttributeOwnerUtil.getQueryAttributes((RsDocAndAttributeOwner) element).getStability();
        if (stability != null) return stability;

        if (element instanceof RsAbstractable) {
            RsAbstractableOwner owner = RsAbstractableOwnerUtil.getOwner((RsAbstractable) element);
            if (owner instanceof RsAbstractableOwner.Impl) {
                RsImplItem impl = ((RsAbstractableOwner.Impl) owner).getImpl();
                RsStability ownerStability = RsDocAndAttributeOwnerUtil.getQueryAttributes(impl).getStability();
                if (ownerStability != RsStability.Stable) {
                    return ownerStability != null ? ownerStability : RsStability.Unstable;
                }
                RsAbstractable superItem = ((RsAbstractable) element).getSuperItem();
                if (superItem != null) {
                    RsStability superStability = RsDocAndAttributeOwnerUtil.getQueryAttributes((RsDocAndAttributeOwner) superItem).getStability();
                    return superStability != null ? superStability : RsStability.Unstable;
                }
            }
        }

        return RsStability.Unstable;
    }

    // --- Wrapper processors ---

    /**
     * Views a base processor as an {@link RsResolveProcessor}. The sub-interface adds only default
     * methods over {@code RsResolveProcessorBase<ScopeEntry>}, so the view is behaviour-preserving.
     */
    @Nonnull
    public static RsResolveProcessor asResolveProcessor(@Nonnull RsResolveProcessorBase<ScopeEntry> processor) {
        if (processor instanceof RsResolveProcessor) {
            return (RsResolveProcessor) processor;
        }
        return new RsResolveProcessor() {
            @Override
            public boolean process(@Nonnull ScopeEntry entry) {
                return processor.process(entry);
            }

            @Nullable
            @Override
            public Set<String> getNames() {
                return processor.getNames();
            }
        };
    }

    @Nonnull
    public static <T extends ScopeEntry> RsResolveProcessorBase<T> wrapWithFilter(
        @Nonnull RsResolveProcessorBase<T> processor,
        @Nonnull Predicate<T> filter
    ) {
        return new RsResolveProcessorBase<T>() {
            @Override
            public boolean process(@Nonnull T entry) {
                return filter.test(entry) && processor.process(entry);
            }

            @Nullable
            @Override
            public Set<String> getNames() {
                return processor.getNames();
            }
        };
    }

    @Nonnull
    public static <T extends ScopeEntry, U extends ScopeEntry> RsResolveProcessorBase<U> wrapWithMapper(
        @Nonnull RsResolveProcessorBase<T> processor,
        @Nonnull Function<U, T> mapper
    ) {
        return new RsResolveProcessorBase<U>() {
            @Override
            public boolean process(@Nonnull U entry) {
                return processor.process(mapper.apply(entry));
            }

            @Nullable
            @Override
            public Set<String> getNames() {
                return processor.getNames();
            }
        };
    }

    // --- Private collector implementations ---

    private static class ResolveVariantsCollector implements RsResolveProcessor {
        private final String referenceName;
        final List<RsElement> result = new SmartList<>();

        ResolveVariantsCollector(String referenceName) {
            this.referenceName = referenceName;
        }

        @Override
        public boolean process(@Nonnull ScopeEntry entry) {
            if (referenceName.equals(entry.getName())) {
                RsElement element = entry.getElement();
                if (!(element instanceof RsDocAndAttributeOwner) || RsDocAndAttributeOwnerUtil.existsAfterExpansionSelf((RsDocAndAttributeOwner) element)) {
                    result.add(element);
                }
            }
            return false;
        }

        @Nullable
        @Override
        public Set<String> getNames() {
            return Collections.singleton(referenceName);
        }
    }

    private static class ResolveVariantsAsScopeEntriesCollector<T extends ScopeEntry> implements RsResolveProcessorBase<T> {
        private final String referenceName;
        final List<T> result = new ArrayList<>();

        ResolveVariantsAsScopeEntriesCollector(String referenceName) {
            this.referenceName = referenceName;
        }

        @Override
        public boolean process(@Nonnull T entry) {
            if (referenceName.equals(entry.getName())) {
                RsElement element = entry.getElement();
                if (!(element instanceof RsDocAndAttributeOwner) || RsDocAndAttributeOwnerUtil.existsAfterExpansionSelf((RsDocAndAttributeOwner) element)) {
                    result.add(entry);
                }
            }
            return false;
        }

        @Nullable
        @Override
        public Set<String> getNames() {
            return Collections.singleton(referenceName);
        }
    }

    private static class SinglePathResolveVariantsCollector implements RsResolveProcessor {
        private final PathResolutionContext ctx;
        private final String referenceName;
        final List<RsPathResolveResult<RsElement>> result = new SmartList<>();

        SinglePathResolveVariantsCollector(PathResolutionContext ctx, String referenceName) {
            this.ctx = ctx;
            this.referenceName = referenceName;
        }

        @Override
        public boolean process(@Nonnull ScopeEntry entry) {
            if (referenceName.equals(entry.getName())) {
                collectPathScopeEntry(ctx, result, entry);
            }
            return false;
        }

        @Nullable
        @Override
        public Set<String> getNames() {
            return Collections.singleton(referenceName);
        }
    }

    private static class MultiplePathsResolveVariantsCollector implements RsResolveProcessor {
        private final PathResolutionContext ctx;
        private final Map<String, List<RsPathResolveResult<RsElement>>> resultByName;

        MultiplePathsResolveVariantsCollector(PathResolutionContext ctx,
                                               Map<String, List<RsPathResolveResult<RsElement>>> resultByName) {
            this.ctx = ctx;
            this.resultByName = resultByName;
        }

        @Override
        public boolean process(@Nonnull ScopeEntry entry) {
            List<RsPathResolveResult<RsElement>> list = resultByName.get(entry.getName());
            if (list != null) {
                collectPathScopeEntry(ctx, list, entry);
            }
            return false;
        }

        @Nullable
        @Override
        public Set<String> getNames() {
            return resultByName.keySet();
        }
    }

    private static class PickFirstScopeEntryCollector implements RsResolveProcessor {
        private final String referenceName;
        ScopeEntry result;

        PickFirstScopeEntryCollector(String referenceName) {
            this.referenceName = referenceName;
        }

        @Override
        public boolean process(@Nonnull ScopeEntry entry) {
            if (referenceName.equals(entry.getName())) {
                RsElement element = entry.getElement();
                if (!(element instanceof RsDocAndAttributeOwner) || RsDocAndAttributeOwnerUtil.existsAfterExpansionSelf((RsDocAndAttributeOwner) element)) {
                    result = entry;
                    return true;
                }
            }
            return false;
        }

        @Nullable
        @Override
        public Set<String> getNames() {
            return Collections.singleton(referenceName);
        }
    }

    private static class NamesCollector implements RsResolveProcessor {
        final Set<String> result = new HashSet<>();

        @Override
        public boolean process(@Nonnull ScopeEntry entry) {
            if (!"_".equals(entry.getName())) {
                result.add(entry.getName());
            }
            return false;
        }

        @Nullable
        @Override
        public Set<String> getNames() {
            return null;
        }
    }

    private static class CompletionVariantsCollector implements RsResolveProcessor {
        private final CompletionResultSet result;
        private final RsCompletionContext context;

        CompletionVariantsCollector(CompletionResultSet result, RsCompletionContext context) {
            this.result = result;
            this.context = context;
        }

        @Override
        public boolean process(@Nonnull ScopeEntry entry) {
            result.addElement(CompletionUtil.createLookupElement(entry, context));
            return false;
        }

        @Nullable
        @Override
        public Set<String> getNames() {
            return null;
        }
    }

    private static void collectPathScopeEntry(
        @Nonnull PathResolutionContext ctx,
        @Nonnull List<RsPathResolveResult<RsElement>> result,
        @Nonnull ScopeEntry e
    ) {
        RsElement element = e.getElement();
        if (element instanceof RsDocAndAttributeOwner && !RsDocAndAttributeOwnerUtil.existsAfterExpansionSelf((RsDocAndAttributeOwner) element)) {
            return;
        }
        VisibilityStatus visibilityStatus = getVisibilityStatusFrom(e, ctx.getContext(), null);
        if (visibilityStatus == VisibilityStatus.CfgDisabled) return;

        boolean isVisible = visibilityStatus == VisibilityStatus.Visible;
        Set<Namespace> namespaces = e.getNamespaces();
        result.add(new RsPathResolveResult<>(
            element,
            org.rust.lang.core.types.infer.FoldUtil.foldTyInferWithTyPlaceholder(e.getSubst()),
            isVisible,
            namespaces));
    }

    /**
     * Filters path-completion assoc-item variants whose impl's obligations cannot be satisfied for
     * the current receiver type. Mirrors the private {@code filterPathCompletionVariantsByTraitBounds}
     */
    @SuppressWarnings("unchecked")
    @Nonnull
    public static RsResolveProcessor filterPathCompletionVariantsByTraitBounds(
        @Nonnull RsResolveProcessor processor,
        @Nonnull ImplLookup lookup
    ) {
        Map<TraitImplSource, Boolean> cache = new HashMap<>();
        return asResolveProcessor(wrapWithFilter(processor, entry -> {
            if (!(entry instanceof AssocItemScopeEntry)) return true;
            AssocItemScopeEntry assoc = (AssocItemScopeEntry) entry;
            Ty receiver = assoc.getSubst().get(TyTypeParameter.self());
            if (receiver == null) return true;
            // Don't filter partially unknown types
            if (FoldUtil.containsTyOfClass(receiver, TyUnknown.class)) return true;
            return cache.computeIfAbsent(assoc.getSource(),
                source -> lookup.getCtx().canEvaluateBounds(source, receiver));
        }));
    }

    /**
     * Deduplicates method completion variants by (name, trait). Mirrors the private
     * <p>
     * There can be multiple impls of the same trait at different dereference levels
     * (e.g. {@code Debug} for {@code str} and for {@code &str}) which produce duplicate
     * lookup entries.
     */
    @SuppressWarnings("unchecked")
    @Nonnull
    public static RsResolveProcessor deduplicateMethodCompletionVariants(
        @Nonnull RsResolveProcessor processor
    ) {
        Set<Map.Entry<String, RsTraitItem>> processed = new HashSet<>();
        return asResolveProcessor(wrapWithFilter(processor, entry -> {
            if (!(entry instanceof MethodResolveVariant)) return true;
            MethodResolveVariant mrv = (MethodResolveVariant) entry;
            BoundElement<RsTraitItem> implementedTrait = mrv.getSource().getImplementedTrait();
            RsTraitItem trait = implementedTrait != null ? implementedTrait.getTypedElement() : null;
            return processed.add(new AbstractMap.SimpleImmutableEntry<>(mrv.getName(), trait));
        }));
    }

    /**
     * Filters method completion variants whose impl's obligations cannot be satisfied for
     * the current receiver. Mirrors {@code filterMethodCompletionVariantsByTraitBounds} in
     */
    @SuppressWarnings("unchecked")
    @Nonnull
    public static RsResolveProcessor filterMethodCompletionVariantsByTraitBounds(
        @Nonnull ImplLookup lookup,
        @Nonnull Ty receiverTy,
        @Nonnull RsResolveProcessor processor
    ) {
        // Don't filter partially unknown types
        if (FoldUtil.containsTyOfClass(receiverTy, TyUnknown.class)) return processor;

        Map<Map.Entry<TraitImplSource, Integer>, Boolean> cache = new HashMap<>();
        return asResolveProcessor(wrapWithFilter(processor, entry -> {
            if (!(entry instanceof MethodResolveVariant)) return true;
            MethodResolveVariant mrv = (MethodResolveVariant) entry;
            Map.Entry<TraitImplSource, Integer> key =
                new AbstractMap.SimpleImmutableEntry<>(mrv.getSource(), mrv.getDerefCount());
            return cache.computeIfAbsent(key,
                k -> lookup.getCtx().canEvaluateBounds(mrv.getSource(), mrv.getSelfTy()));
        }));
    }
}
