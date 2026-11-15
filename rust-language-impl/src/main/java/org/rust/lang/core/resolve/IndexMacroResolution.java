/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.StubBasedPsiElement;
import consulo.language.psi.stub.StubElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.macros.RsExpandedElement;
import org.rust.lang.core.macros.RsExpandedElementUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsItemsOwner;
import org.rust.lang.core.psi.ext.RsMacroDefinitionBase;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.RsPossibleMacroCall;
import org.rust.lang.core.psi.ext.impl.PsiElementUtil;
import org.rust.lang.core.psi.ext.impl.RsDocAndAttributeOwnerUtil;
import org.rust.lang.core.psi.ext.impl.RsElementUtil;
import org.rust.lang.core.psi.ext.impl.RsExternCrateItemUtil;
import org.rust.lang.core.psi.ext.impl.RsItemsOwnerUtil;
import org.rust.lang.core.psi.ext.impl.RsMacroCallUtil;
import org.rust.lang.core.psi.ext.impl.RsModDeclItemUtil;
import org.rust.lang.core.psi.ext.impl.RsModItemUtil;
import org.rust.lang.core.psi.impl.RsPsiManagerUtil;
import org.rust.lang.core.psi.ext.impl.RsUseSpeckUtil;
import org.rust.lang.core.psi.impl.RsFile;
import org.rust.lang.core.resolve2.ItemProcessingMode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolution of an unqualified macro name - {@code foo!()} - without a def map, in textual order: the
 * {@code macro_rules!} written before the call in its file, and before the declaration of each module on the way up to
 * the crate root, the macros of {@code #[macro_use]} modules and crates met on the way, the macros {@code use} items
 * import from other crates, then the {@code #[macro_export]} macros of the crate and the macros of the standard crate.
 */
public final class IndexMacroResolution {
    private IndexMacroResolution() {
    }

    /** The macros named as {@code processor} asks, visible where {@code start} is, nearest first. */
    public static boolean processMacrosInScope(@Nonnull PsiElement start, boolean ignoreLegacyMacros, @Nonnull RsResolveProcessor processor) {
        Walk walk = new Walk(processor, ignoreLegacyMacros);
        PsiElement element = start;
        while (element != null) {
            // an expanded item is in the scope of the macro call it came from
            if (element instanceof RsExpandedElement expanded) {
                RsPossibleMacroCall call = RsExpandedElementUtil.getExpandedFrom(expanded);
                if (call != null && call != element) {
                    element = call;
                    continue;
                }
            }
            // the siblings by the stubs when the element has one: walking the PSI would parse every file on the way up
            StubElement<?> stub = element instanceof StubBasedPsiElement<?> stubBased ? PsiElementUtil.getGreenStubOf(stubBased) : null;
            StubElement<?> parentStub = stub != null ? stub.getParentStub() : null;
            if (parentStub != null) {
                List<StubElement> siblings = parentStub.getChildrenStubs();
                for (int i = siblings.indexOf(stub) - 1; i >= 0; i--) {
                    if (walk.visit(siblings.get(i).getPsi())) return true;
                }
            }
            else {
                for (PsiElement sibling = element.getPrevSibling(); sibling != null; sibling = sibling.getPrevSibling()) {
                    if (walk.visit(sibling)) return true;
                }
            }
            PsiElement context = element.getContext();
            if (context instanceof RsFile file) {
                // the macros defined before `mod file;` in the parent module are in scope in the file
                element = file.isCrateRoot() ? null : file.getDeclaration();
            }
            else {
                element = context;
            }
        }
        if (walk.processImportedMacros()) return true;
        if (walk.processUsedMacros()) return true;

        if (!(start instanceof RsElement || start.getContext() instanceof RsElement)) return false;
        RsElement startElement = start instanceof RsElement e ? e : (RsElement) start.getContext();
        RsFile crateRoot = RsElementUtil.getContainingCrate(startElement).getRootMod();
        if (crateRoot == null) return false;
        if (IndexItemResolution.processCrateExportedMacros(crateRoot, processor.getNames(), processor)) return true;
        // the standard crate is in scope as if by `#[macro_use] extern crate std`
        String stdlibName = crateRoot.getStdlibAttributes().getAutoInjectedCrate();
        RsFile stdlibRoot = stdlibName != null ? IndexItemResolution.findDependencyCrateRoot(crateRoot, stdlibName) : null;
        if (stdlibRoot != null && processCrateMacros(stdlibRoot, processor)) return true;
        // `include!`, `concat!`, `env!` and the other built-in macros come with the prelude
        RsMod prelude = IndexItemResolution.findPrelude(startElement);
        return prelude != null && IndexItemResolution.processItemDeclarations(prelude, Namespace.MACROS, processor,
            ItemProcessingMode.WITHOUT_PRIVATE_IMPORTS);
    }

    /** The macros another crate offers: its exported ones and the ones its root re-exports by `pub use`. */
    private static boolean processCrateMacros(@Nonnull RsFile crateRoot, @Nonnull RsResolveProcessor processor) {
        return IndexItemResolution.processItemDeclarations(crateRoot, Namespace.MACROS, processor, ItemProcessingMode.WITHOUT_PRIVATE_IMPORTS);
    }

    private static boolean processCrateMacros(@Nonnull RsFile crateRoot, @Nullable Set<String> names, @Nonnull RsResolveProcessor processor) {
        if (names == null || names.equals(processor.getNames())) return processCrateMacros(crateRoot, processor);
        return processCrateMacros(crateRoot, Processors.asResolveProcessor(new RsResolveProcessorBase<ScopeEntry>() {
            @Override
            public boolean process(@Nonnull ScopeEntry entry) {
                return processor.process(entry);
            }

            @Nullable
            @Override
            public Set<String> getNames() {
                return names;
            }
        }));
    }

    /** What the walk meets on its way up. */
    private static final class Walk {
        @Nonnull
        private final RsResolveProcessor processor;
        private final boolean ignoreLegacyMacros;
        /** The crates `extern crate` named without `#[macro_use]`: `use krate::m` imports their macros. */
        @Nonnull
        private final Map<String, RsFile> externCrates = new LinkedHashMap<>();
        @Nonnull
        private final List<RsUseItem> useItems = new ArrayList<>();

        Walk(@Nonnull RsResolveProcessor processor, boolean ignoreLegacyMacros) {
            this.processor = processor;
            this.ignoreLegacyMacros = ignoreLegacyMacros;
        }

        boolean visit(@Nonnull PsiElement element) {
            if (element instanceof RsMacro macro) {
                return !ignoreLegacyMacros && processMacro(macro);
            }
            if (element instanceof RsModItem module) {
                if (ignoreLegacyMacros || !RsModItemUtil.getHasMacroUse(module) || !exists(module)) return false;
                return processAllReversed(visibleMacros(module));
            }
            if (element instanceof RsModDeclItem declaration) {
                if (ignoreLegacyMacros || !RsModDeclItemUtil.getHasMacroUse(declaration) || !exists(declaration)) return false;
                PsiElement module = declaration.getReference().resolve();
                return module instanceof RsMod mod && processAllReversed(visibleMacros(mod));
            }
            if (element instanceof RsExternCrateItem externCrate) {
                if (!exists(externCrate)) return false;
                PsiReference reference = externCrate.getReference();
                PsiElement crateRoot = reference != null ? reference.resolve() : null;
                if (!(crateRoot instanceof RsFile root)) return false;
                if (RsExternCrateItemUtil.getHasMacroUse(externCrate)) {
                    return processCrateMacros(root, processor);
                }
                externCrates.put(RsExternCrateItemUtil.getNameWithAlias(externCrate), root);
                return false;
            }
            if (element instanceof RsUseItem useItem) {
                useItems.add(useItem);
            }
            return false;
        }

        /** `use krate::m;` and `use krate::*;`: macros exported by another crate. */
        boolean processImportedMacros() {
            for (RsUseItem useItem : useItems) {
                RsUseSpeck root = useItem.getUseSpeck();
                if (root == null || !exists(useItem)) continue;
                List<RsUseSpeck> leaves = new ArrayList<>();
                RsUseSpeckUtil.forEachLeafSpeck(root, leaves::add);
                for (RsUseSpeck speck : leaves) {
                    RsPath crate = importedCrate(speck);
                    if (crate == null) continue;
                    RsPath path = speck.getPath();
                    String crateName = crate.getReferenceName();
                    RsFile crateRoot = externCrates.get(crateName);
                    if (crateRoot == null) crateRoot = IndexItemResolution.findDependencyCrateRoot(useItem, crateName);
                    if (crateRoot == null) continue;
                    Set<String> names = processor.getNames();
                    if (!RsUseSpeckUtil.isStarImport(speck)) {
                        String macroName = path != null ? path.getReferenceName() : null;
                        if (macroName == null || names != null && !names.contains(RsUseSpeckUtil.getNameInScope(speck))) continue;
                        names = Set.of(macroName);
                    }
                    if (processCrateMacros(crateRoot, names, processor)) return true;
                }
            }
            return false;
        }

        /** `use some::path::m;`: a macro imported by its path, like any other item since 2018. */
        boolean processUsedMacros() {
            Set<String> names = processor.getNames();
            for (RsUseItem useItem : useItems) {
                RsUseSpeck root = useItem.getUseSpeck();
                if (root == null || !exists(useItem)) continue;
                List<RsUseSpeck> leaves = new ArrayList<>();
                RsUseSpeckUtil.forEachLeafSpeck(root, leaves::add);
                for (RsUseSpeck speck : leaves) {
                    if (RsUseSpeckUtil.isStarImport(speck)) continue;
                    String name = RsUseSpeckUtil.getNameInScope(speck);
                    RsPath path = speck.getPath();
                    if (name == null || path == null || names != null && !names.contains(name)) continue;
                    PsiReference reference = path.getReference();
                    if (!(reference instanceof consulo.language.psi.PsiPolyVariantReference poly)) continue;
                    for (consulo.language.psi.ResolveResult result : poly.multiResolve(false)) {
                        PsiElement target = result.getElement();
                        boolean isMacro = target instanceof RsMacroDefinitionBase
                            || target instanceof RsFunction function && org.rust.lang.core.psi.ext.impl.RsFunctionUtil.isProcMacroDef(function);
                        if (isMacro && Processors.processEntry(processor, name, Namespace.MACROS, (RsElement) target)) return true;
                    }
                }
            }
            return false;
        }

        /**
         * The crate of a two-segment import - {@code use krate::m}, {@code use krate::{m}} or {@code use krate::*} -
         * or {@code null} for any other: resolving longer paths here would resolve macros while resolving a macro.
         */
        @Nullable
        private static RsPath importedCrate(@Nonnull RsUseSpeck speck) {
            RsPath path = speck.getPath();
            RsPath groupQualifier = RsUseSpeckUtil.getQualifier(speck);
            RsPath crate;
            if (RsUseSpeckUtil.isStarImport(speck)) {
                crate = path != null ? path : groupQualifier;
                if (path != null && groupQualifier != null) return null;
            }
            else if (path == null) {
                return null;
            }
            else if (path.getPath() != null) {
                if (groupQualifier != null) return null;
                crate = path.getPath();
            }
            else {
                crate = groupQualifier;
            }
            return crate != null && crate.getPath() == null ? crate : null;
        }

        private boolean processMacro(@Nonnull RsMacro macro) {
            String name = macro.getName();
            if (name == null || !exists(macro)) return false;
            Set<String> names = processor.getNames();
            if (names != null && !names.contains(name)) return false;
            return Processors.processEntry(processor, name, Namespace.MACROS, macro);
        }

        private boolean processAllReversed(@Nonnull List<RsMacro> macros) {
            for (int i = macros.size() - 1; i >= 0; i--) {
                if (processMacro(macros.get(i))) return true;
            }
            return false;
        }
    }

    /** The `macro_rules!` a `#[macro_use]` module makes visible after it, in textual order: its own and its `#[macro_use]` children's. */
    @Nonnull
    private static List<RsMacro> visibleMacros(@Nonnull RsMod module) {
        return CachedValuesManager.getManager(module.getProject()).getCachedValue(module, () -> {
            List<RsMacro> result = new ArrayList<>();
            collectVisibleMacros(module, result);
            return CachedValueProvider.Result.create(result, RsPsiManagerUtil.getRustStructureModificationTracker(module.getProject()));
        });
    }

    private static void collectVisibleMacros(@Nonnull RsItemsOwner scope, @Nonnull List<RsMacro> result) {
        for (RsElement element : RsItemsOwnerUtil.getItemsAndMacros(scope)) {
            collectVisibleMacros(element, result);
        }
    }

    /**
     * The `macro_rules!` an item of a `#[macro_use]` module leaves in scope after it - those that macro calls write as
     * well: yansi's `define_properties! { .. }` writes the `fg!` its `properties!` calls.
     */
    private static void collectVisibleMacros(@Nonnull PsiElement element, @Nonnull List<RsMacro> result) {
        if (element instanceof RsMacro macro) {
            result.add(macro);
        }
        else if (element instanceof RsMacroCall call && exists(call)) {
            RsMacroCallUtil.processExpansionRecursively(call, expanded -> {
                if (!(expanded instanceof RsMacroCall)) collectVisibleMacros(expanded, result);
                return false;
            });
        }
        else if (element instanceof RsModItem module && RsModItemUtil.getHasMacroUse(module) && exists(module)) {
            collectVisibleMacros(module, result);
        }
        else if (element instanceof RsModDeclItem declaration && RsModDeclItemUtil.getHasMacroUse(declaration) && exists(declaration)) {
            if (declaration.getReference().resolve() instanceof RsMod module) collectVisibleMacros(module, result);
        }
    }

    private static boolean exists(@Nonnull PsiElement element) {
        return !(element instanceof org.rust.lang.core.psi.ext.RsDocAndAttributeOwner owner)
            || RsDocAndAttributeOwnerUtil.existsAfterExpansionSelf(owner);
    }

    /**
     * The known kind of the attribute macro {@code path} names - {@code #[async_trait]}, {@code #[tracing::instrument]} -
     * by the package and name of the procedural macro it resolves to; {@code null} when it is none of the known ones.
     */
    @Nullable
    public static org.rust.lang.core.psi.impl.KnownProcMacroKind resolveKnownAttributeMacroKind(@Nonnull RsPath path) {
        String name = path.getReferenceName();
        if (name == null) return null;
        RsFunction[] found = {null};
        RsResolveProcessor processor = new RsResolveProcessor() {
            @Override
            public boolean process(@Nonnull ScopeEntry entry) {
                if (entry.getElement() instanceof RsFunction function
                    && org.rust.lang.core.psi.ext.impl.RsFunctionUtil.isProcMacroDef(function)) {
                    found[0] = function;
                    return true;
                }
                return false;
            }

            @Nullable
            @Override
            public Set<String> getNames() {
                return Set.of(name);
            }
        };
        RsPath qualifier = path.getPath();
        if (qualifier == null) {
            processMacrosInScope(path, true, processor);
        }
        else {
            PsiReference reference = qualifier.getReference();
            if (reference != null && reference.resolve() instanceof RsMod module) {
                IndexItemResolution.processItemDeclarations(module, Namespace.MACROS, processor, ItemProcessingMode.WITHOUT_PRIVATE_IMPORTS);
            }
        }
        if (found[0] == null) return null;
        String macroName = org.rust.lang.core.psi.ext.impl.RsFunctionUtil.getProcMacroName(found[0]);
        if (macroName == null) return null;
        // by the name the crate is known as in code: `async_trait`, not the package `async-trait`
        String crateName = RsElementUtil.getContainingCrate(found[0]).getNormName();
        return org.rust.lang.core.psi.impl.HardcodedProcMacroProperties.getHardcodeProcMacroProperties(crateName, macroName);
    }

    /** The definition {@code call} names, found without a def map: what its path resolves to. */
    @Nullable
    public static PsiElement resolveMacroCall(@Nonnull RsMacroCall call) {
        PsiReference reference = call.getPath().getReference();
        return reference != null ? reference.resolve() : null;
    }
}
