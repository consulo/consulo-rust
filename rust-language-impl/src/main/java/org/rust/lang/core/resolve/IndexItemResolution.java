/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.util.dataholder.Key;
import consulo.application.util.RecursionGuard;
import consulo.application.util.RecursionManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.ResolveResult;
import consulo.language.psi.PsiPolyVariantReference;
import consulo.language.psi.PsiReference;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.scope.GlobalSearchScopesCore;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.api.util.AutoInjectedCrates;
import org.rust.cargo.api.workspace.CargoWorkspace;
import org.rust.cargo.api.workspace.PackageOrigin;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsItemElement;
import org.rust.lang.core.psi.ext.RsItemsOwner;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.RsNamedElement;
import org.rust.lang.core.psi.ext.RsVisibilityOwner;
import org.rust.lang.core.psi.ext.RsVisible;
import org.rust.lang.core.psi.ext.impl.RsCachedItems;
import org.rust.lang.core.psi.ext.impl.RsDocAndAttributeOwnerUtil;
import org.rust.lang.core.psi.ext.impl.RsElementUtil;
import org.rust.lang.core.psi.ext.impl.RsEnumItemUtil;
import org.rust.lang.core.psi.ext.impl.RsFunctionUtil;
import org.rust.lang.core.psi.ext.impl.PsiElementUtil;
import org.rust.lang.core.psi.ext.impl.RsItemsOwnerUtil;
import org.rust.lang.core.psi.ext.impl.RsMacroCallUtil;
import org.rust.lang.core.psi.ext.impl.RsMacroUtil;
import org.rust.lang.core.psi.ext.impl.RsUseSpeckUtil;
import org.rust.lang.core.psi.ext.impl.RsVisibilityUtil;
import org.rust.lang.core.psi.impl.RsFile;
import org.rust.lang.core.psi.impl.RsPsiUtilUtil;
import org.rust.lang.core.resolve.indexes.RsImplMacroCallIndex;
import org.rust.lang.core.resolve.indexes.RsMacroIndex;
import org.rust.lang.core.resolve2.ItemProcessingMode;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Name resolution of the items of a module without a def map: a name is looked up where it is asked for, the way the
 * stub indexes answer it, and nothing else of the crate is computed.
 * <p>
 * A module offers the items it declares - its macro expansions included, see {@link RsItemsOwnerUtil#getExpandedItemsCached}
 * - then its named imports, whose paths are resolved only for the names asked for, then the names of the crates it
 * depends on, then its glob imports, which ask the same name of the module they import. The prelude is looked up the
 * same way, as {@code prelude::rust_20xx} of the standard crate.
 * <p>
 * This is how name resolution worked before def maps; a def map collects every module of every crate up front, which is
 * what made the first resolution after opening a project take seconds.
 */
public final class IndexItemResolution {
    private static final RecursionGuard<PsiElement> GLOB_GUARD = RecursionManager.createGuard("IndexItemResolution.glob");

    private IndexItemResolution() {
    }

    private static final boolean ENABLED = !"false".equals(System.getProperty("rust.resolve.index"));

    /** Whether name resolution uses this instead of the def maps - the default; {@code -Drust.resolve.index=false} for the def maps. */
    public static boolean isEnabled() {
        return ENABLED;
    }

    /** The items {@code scope} declares, imports and gets from its dependencies, in that order of precedence. */
    public static boolean processItemDeclarations(
        @Nonnull RsItemsOwner scope,
        @Nonnull Set<Namespace> ns,
        @Nonnull RsResolveProcessor processor,
        @Nonnull ItemProcessingMode ipm
    ) {
        boolean withPrivateImports = ipm != ItemProcessingMode.WITHOUT_PRIVATE_IMPORTS;
        Set<String> names = processor.getNames();
        RsCachedItems items = RsItemsOwnerUtil.getExpandedItemsCached(scope);
        // the names declared or imported by name, which shadow the ones of glob imports and of the extern prelude
        Set<String> declared = new HashSet<>();

        for (Map.Entry<String, List<RsItemElement>> entry : namedEntries(items.getNamed(), names)) {
            String name = entry.getKey();
            for (RsItemElement item : entry.getValue()) {
                if (processItem(name, item, ns, processor, withPrivateImports, declared)) return true;
            }
        }

        // the procedural macros of a proc-macro crate are among its named items already
        if (scope instanceof RsFile file && file.isCrateRoot() && ns.contains(Namespace.Macros)
            && !RsElementUtil.getContainingCrate(file).getKind().isProcMacro()) {
            if (processExportedMacros(file, names, processor, declared)) return true;
        }

        ImportTable imports = importTable(scope);
        boolean isAtLeastEdition2018 = isAtLeastEdition2018(scope);
        for (NamedImport imported : imports.named(names)) {
            if (!(imported.isPublic() || withPrivateImports)) continue;
            String name = imported.name();
            RsPath path = imported.path();
            // `pub(crate) use m;` re-exports the `macro_rules! m` written above it
            if (imported.isSingleName() && ns.contains(Namespace.Macros) && processLegacyMacro(items, path.getReferenceName(), name, processor, declared)) {
                return true;
            }
            if (isAtLeastEdition2018 && imported.isSingleName()) {
                // `use foo;` names a crate since 2018, and only a `pub use` of it means anything to other modules
                if (!withPrivateImports && ns.contains(Namespace.Types)) {
                    RsFile crateRoot = findDependencyCrateRoot(scope, path.getReferenceName());
                    if (crateRoot != null && Processors.processEntry(processor, name, Namespace.TYPES, crateRoot)) return true;
                }
                continue;
            }
            RsPath importedPath = imported.importedPath();
            if (importedPath != null && processImport(name, importedPath, imported.useItem(), ns, processor, declared)) return true;
        }

        if (ipm.isWithExternCrates() && ns.contains(Namespace.Types) && scope instanceof RsMod mod) {
            RsResolveProcessor notShadowed = notShadowed(processor, declared);
            // since 2018 the `extern crate` items of the crate root are in scope in every module of the crate
            if (isAtLeastEdition2018 && !mod.isCrateRoot() && mod.getCrateRoot() instanceof RsFile crateRoot) {
                if (processRootExternCrates(crateRoot, names, notShadowed)) return true;
            }
            if (processExternCrateResolveVariants(mod, false, notShadowed)) return true;
        }

        for (StarImport star : imports.stars()) {
            if (!(star.isPublic() || withPrivateImports)) continue;
            if (processStarImport(scope, star.speck(), ns, notShadowed(processor, declared), withPrivateImports)) return true;
        }
        return false;
    }

    /**
     * The `use` items of a module, leaf by leaf, by the name each brings into scope. Built once per structure change:
     * a lookup of one name otherwise walks the trees of all the imports of the module.
     */
    @Nonnull
    private static ImportTable importTable(@Nonnull RsItemsOwner scope) {
        return CachedValuesManager.getManager(scope.getProject()).getCachedValue(scope, IMPORT_TABLE_KEY, () -> {
            Map<String, List<NamedImport>> byName = new LinkedHashMap<>();
            List<NamedImport> all = new ArrayList<>();
            List<StarImport> stars = new ArrayList<>();
            for (RsUseItem useItem : RsItemsOwnerUtil.getExpandedItemsCached(scope).getImports()) {
                if (!RsDocAndAttributeOwnerUtil.existsAfterExpansionSelf(useItem)) continue;
                RsUseSpeck root = useItem.getUseSpeck();
                if (root == null) continue;
                boolean isPublic = isPublic(useItem);
                List<RsUseSpeck> leaves = new ArrayList<>();
                RsUseSpeckUtil.forEachLeafSpeck(root, leaves::add);
                for (RsUseSpeck speck : leaves) {
                    if (RsUseSpeckUtil.isStarImport(speck)) {
                        stars.add(new StarImport(speck, isPublic));
                        continue;
                    }
                    String name = RsUseSpeckUtil.getNameInScope(speck);
                    RsPath path = speck.getPath();
                    if (name == null || path == null) continue;
                    // a single name, maybe renamed: `use foo;`, `use foo as bar;`
                    boolean isSingleName = path.getPath() == null && !path.getHasColonColon() && RsUseSpeckUtil.getQualifier(speck) == null;
                    // `use foo::bar::{self}` imports `foo::bar` itself
                    RsPath importedPath = "self".equals(path.getReferenceName()) && path.getPath() == null
                        ? RsUseSpeckUtil.getQualifier(speck) : path;
                    NamedImport imported = new NamedImport(name, useItem, path, importedPath, isSingleName, isPublic);
                    byName.computeIfAbsent(name, k -> new ArrayList<>()).add(imported);
                    all.add(imported);
                }
            }
            return CachedValueProvider.Result.create(new ImportTable(byName, all, stars), RsPsiUtilUtil.getRustStructureOrAnyPsiModificationTracker(scope));
        }, false);
    }

    private static final Key<CachedValue<ImportTable>> IMPORT_TABLE_KEY = Key.create("org.rust.lang.core.resolve.IndexItemResolution.importTable");

    private record ImportTable(@Nonnull Map<String, List<NamedImport>> byName, @Nonnull List<NamedImport> all, @Nonnull List<StarImport> stars) {
        /** The imports of {@code names}, in the order they are written, or all of them. */
        @Nonnull
        List<NamedImport> named(@Nullable Set<String> names) {
            if (names == null) return all;
            if (names.size() == 1) return byName.getOrDefault(names.iterator().next(), List.of());
            List<NamedImport> result = new ArrayList<>();
            for (NamedImport imported : all) {
                if (names.contains(imported.name())) result.add(imported);
            }
            return result;
        }
    }

    /** `use path as name`: {@code importedPath} is the path imported, the qualifier of `{self}`. */
    private record NamedImport(@Nonnull String name, @Nonnull RsUseItem useItem, @Nonnull RsPath path, @Nullable RsPath importedPath,
                               boolean isSingleName, boolean isPublic) {
    }

    private record StarImport(@Nonnull RsUseSpeck speck, boolean isPublic) {
    }

    private static boolean processItem(
        @Nonnull String name,
        @Nonnull RsItemElement item,
        @Nonnull Set<Namespace> ns,
        @Nonnull RsResolveProcessor processor,
        boolean withPrivateImports,
        @Nonnull Set<String> declared
    ) {
        if (item instanceof RsExternCrateItem externCrate) {
            if (!(externCrate.isPublic() || withPrivateImports) || !ns.contains(Namespace.Types) || "self".equals(name)) return false;
            PsiReference reference = externCrate.getReference();
            PsiElement crateRoot = reference != null ? reference.resolve() : null;
            if (crateRoot == null) return false;
            declared.add(name);
            return Processors.processEntry(processor, name, (RsElement) crateRoot, Namespace.TYPES, visibleAs(externCrate));
        }
        if (item instanceof RsModDeclItem modDecl) {
            if (!ns.contains(Namespace.Types)) return false;
            PsiElement mod = modDecl.getReference().resolve();
            if (!(mod instanceof RsElement module)) return false;
            declared.add(name);
            return Processors.processEntry(processor, name, module, Namespace.TYPES, visibleAs(modDecl));
        }
        if (!(item instanceof RsNamedElement named)) return false;
        // a procedural macro is a function, but its name is the macro's
        Set<Namespace> itemNs = item instanceof RsFunction function && RsFunctionUtil.isProcMacroDef(function)
            ? Namespace.MACROS : Namespace.getNamespaces(named);
        if (!intersects(itemNs, ns)) return false;
        declared.add(name);
        return Processors.processEntry(processor, name, named, itemNs, visibleAs(item));
    }

    /**
     * Whether an entry is visible from the module of the path being resolved, by the visibility its item or import
     * is declared with: `use futures::future::try_join_all` names both the private module and the function it
     * re-exports, and only the function is visible from another crate.
     */
    @Nonnull
    private static VisibilityFilter visibleAs(@Nonnull RsVisible declaration) {
        return (context, lazyModInfo) -> {
            RsMod mod = context.getContainingMod();
            return mod == null || RsVisibilityUtil.isVisibleFrom(declaration, mod) ? VisibilityStatus.Visible : VisibilityStatus.Invisible;
        };
    }

    /** `use path as name`: what the path resolves to in the namespaces asked for, each namespace once. */
    private static boolean processImport(
        @Nonnull String name,
        @Nonnull RsPath path,
        @Nonnull RsUseItem useItem,
        @Nonnull Set<Namespace> ns,
        @Nonnull RsResolveProcessor processor,
        @Nonnull Set<String> declared
    ) {
        PsiReference reference = path.getReference();
        if (reference == null) return false;
        Set<Namespace> visited = EnumSet.noneOf(Namespace.class);
        for (PsiElement target : multiResolve(reference)) {
            if (!(target instanceof RsNamedElement named)) continue;
            Set<Namespace> targetNs = Namespace.getNamespaces(named);
            if (!intersects(targetNs, ns) || visited.containsAll(targetNs)) continue;
            visited.addAll(targetNs);
            declared.add(name);
            if (Processors.processEntry(processor, name, named, targetNs, visibleAs(useItem))) return true;
        }
        return false;
    }

    /** `use path::*`: the same names asked of the module or enum the path names. */
    private static boolean processStarImport(
        @Nonnull RsItemsOwner scope,
        @Nonnull RsUseSpeck speck,
        @Nonnull Set<Namespace> ns,
        @Nonnull RsResolveProcessor processor,
        boolean withPrivateImports
    ) {
        RsPath path = speck.getPath();
        // `use foo::bar::{self, *}`: the `*` of the group imports from the qualifier
        RsPath basePath = path == null && speck.getContext() instanceof RsUseGroup ? RsUseSpeckUtil.getQualifier(speck) : path;
        PsiElement target;
        if (basePath != null) {
            PsiReference reference = basePath.getReference();
            target = reference != null ? reference.resolve() : null;
        }
        else {
            target = scope instanceof RsElement element ? RsElementUtil.getContainingCrate(element).getRootMod() : null;
        }
        if (target == null) return false;
        PsiElement imported = target;
        // a glob that leads back to a module already being looked into adds nothing new
        Boolean found = GLOB_GUARD.doPreventingRecursion(imported, false, () -> {
            if (imported instanceof RsEnumItem enumItem) {
                for (RsEnumVariant variant : RsEnumItemUtil.getVariants(enumItem)) {
                    String name = variant.getName();
                    if (name == null) continue;
                    Set<Namespace> variantNs = Namespace.getNamespaces(variant);
                    if (intersects(variantNs, ns) && Processors.processEntry(processor, name, variantNs, variant)) return true;
                }
                return false;
            }
            if (imported instanceof RsMod module) {
                // the private imports of a module are visible from the modules inside it
                boolean seesPrivate = withPrivateImports && isAncestorModule(module, scope);
                return processItemDeclarations(module, ns, processor,
                    seesPrivate ? ItemProcessingMode.WITH_PRIVATE_IMPORTS : ItemProcessingMode.WITHOUT_PRIVATE_IMPORTS);
            }
            return false;
        });
        return found != null && found;
    }

    private static boolean processLegacyMacro(
        @Nonnull RsCachedItems items,
        @Nullable String macroName,
        @Nonnull String name,
        @Nonnull RsResolveProcessor processor,
        @Nonnull Set<String> declared
    ) {
        for (RsMacro macro : items.getLegacyMacros()) {
            if (macroName != null && macroName.equals(macro.getName())) {
                declared.add(name);
                return Processors.processEntry(processor, name, Namespace.MACROS, macro);
            }
        }
        return false;
    }

    private static boolean processRootExternCrates(@Nonnull RsFile crateRoot, @Nullable Set<String> names, @Nonnull RsResolveProcessor processor) {
        for (Map.Entry<String, List<RsItemElement>> entry : namedEntries(RsItemsOwnerUtil.getExpandedItemsCached(crateRoot).getNamed(), names)) {
            for (RsItemElement item : entry.getValue()) {
                if (item instanceof RsExternCrateItem && processItem(entry.getKey(), item, Namespace.TYPES, processor, true, new HashSet<>())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * The crates {@code element}'s crate depends on, by the names it gives them, with {@code self} for its own root when
     * {@code withSelf}: the dependencies declared in the manifest come before the standard ones.
     */
    public static boolean processExternCrateResolveVariants(@Nonnull RsElement element, boolean withSelf, @Nonnull RsResolveProcessor processor) {
        Crate crate = RsElementUtil.getContainingCrate(element);
        if (withSelf) {
            RsFile root = crate.getRootMod();
            if (root != null && Processors.processEntry(processor, "self", Namespace.TYPES, root)) return true;
        }
        Set<String> names = processor.getNames();
        List<Crate.Dependency> dependencies = new ArrayList<>(crate.getDependenciesWithCyclic());
        dependencies.sort((a, b) -> Boolean.compare(isStdlib(a.getCrate()), isStdlib(b.getCrate())));
        Set<String> visited = new HashSet<>();
        for (Crate.Dependency dependency : dependencies) {
            String name = dependency.getNormName();
            if (names != null && !names.contains(name)) continue;
            if (!visited.add(name)) continue;
            RsFile root = dependency.getCrate().getRootMod();
            if (root != null && Processors.processEntry(processor, name, Namespace.TYPES, root)) return true;
        }
        return false;
    }

    /** The root of the crate {@code context}'s crate knows as {@code name}. */
    @Nullable
    public static RsFile findDependencyCrateRoot(@Nonnull RsElement context, @Nullable String name) {
        if (name == null) return null;
        Crate crate = RsElementUtil.getContainingCrate(context);
        for (Crate.Dependency dependency : crate.getDependenciesWithCyclic()) {
            if (name.equals(dependency.getNormName())) return dependency.getCrate().getRootMod();
        }
        return null;
    }

    /**
     * The prelude of {@code element}'s crate: {@code prelude::rust_20xx} of the standard crate it gets implicitly - or of
     * itself, for {@code std} and {@code core} - for the edition of the crate.
     */
    @Nullable
    public static RsMod findPrelude(@Nonnull RsElement element) {
        Crate crate = RsElementUtil.getContainingCrate(element);
        RsFile crateRoot = crate.getRootMod();
        if (crateRoot == null) return null;
        RsFile stdlibRoot;
        String crateName = crate.getNormName();
        if (crate.getOrigin() == PackageOrigin.STDLIB && (AutoInjectedCrates.STD.equals(crateName) || AutoInjectedCrates.CORE.equals(crateName))) {
            stdlibRoot = crateRoot;
        }
        else {
            stdlibRoot = switch (crateRoot.getStdlibAttributes(crate)) {
                case NO_CORE -> null;
                case NO_STD -> findDependencyCrateRoot(crateRoot, AutoInjectedCrates.CORE);
                case NONE -> findDependencyCrateRoot(crateRoot, AutoInjectedCrates.STD);
            };
        }
        if (stdlibRoot == null) return null;
        RsMod prelude = findChildModule(stdlibRoot, "prelude");
        if (prelude == null) return null;
        RsMod editionPrelude = findChildModule(prelude, "rust_" + crate.getEdition().getPresentation());
        return editionPrelude != null ? editionPrelude : findChildModule(prelude, "v1");
    }

    @Nullable
    private static RsMod findChildModule(@Nonnull RsMod parent, @Nonnull String name) {
        RsMod[] found = {null};
        RsResolveProcessor processor = new RsResolveProcessor() {
            @Override
            public boolean process(@Nonnull ScopeEntry entry) {
                if (entry.getElement() instanceof RsMod mod) {
                    found[0] = mod;
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
        processItemDeclarations(parent, Namespace.TYPES, processor, ItemProcessingMode.WITH_PRIVATE_IMPORTS);
        return found[0];
    }

    /** The {@code #[macro_export]} macros of the crate of {@code crateRoot}, which sit in its root. */
    private static boolean processExportedMacros(
        @Nonnull RsFile crateRoot,
        @Nullable Set<String> names,
        @Nonnull RsResolveProcessor processor,
        @Nonnull Set<String> declared
    ) {
        return processCrateExportedMacros(crateRoot, names, Processors.asResolveProcessor(Processors.wrapWithFilter(processor, entry -> {
            declared.add(entry.getName());
            return true;
        })));
    }

    /**
     * The macros the crate of {@code crateRoot} exports: its {@code #[macro_export]} macros, found in the index for the
     * files of its package, or the procedural macros of a proc-macro crate.
     */
    public static boolean processCrateExportedMacros(
        @Nonnull RsFile crateRoot,
        @Nullable Set<String> names,
        @Nonnull RsResolveProcessor processor
    ) {
        Crate crate = RsElementUtil.getContainingCrate(crateRoot);
        if (crate.getKind().isProcMacro()) {
            for (RsFunction function : PsiElementUtil.stubChildrenOfType(crateRoot, RsFunction.class)) {
                if (!RsFunctionUtil.isProcMacroDef(function)) continue;
                String name = RsFunctionUtil.getProcMacroName(function);
                if (name == null || (names != null && !names.contains(name))) continue;
                if (Processors.processEntry(processor, name, Namespace.MACROS, function)) return true;
            }
            return false;
        }
        CargoWorkspace.Target target = crate.getCargoTarget();
        VirtualFile packageRoot = target != null ? target.getPkg().getContentRoot() : null;
        if (packageRoot == null) return false;
        GlobalSearchScope scope = GlobalSearchScopesCore.directoryScope(crateRoot.getProject(), packageRoot, true);
        for (RsMacro macro : RsMacroIndex.exportedMacros(crateRoot.getProject(), scope)) {
            String name = macro.getName();
            if (name == null || (names != null && !names.contains(name))) continue;
            if (RsElementUtil.getContainingCrate(macro) != crate) continue;
            if (Processors.processEntry(processor, name, Namespace.MACROS, macro)) return true;
        }
        // a macro another macro writes, `doc! { #[macro_export] macro_rules! select { .. } }`: the calls that name it
        if (names != null) {
            for (String name : names) {
                for (RsMacroCall call : RsImplMacroCallIndex.callsMentioning(crateRoot.getProject(), name, scope)) {
                    String body = RsMacroCallUtil.getMacroBody(call);
                    if (body == null || !body.contains("macro_rules") || RsElementUtil.getContainingCrate(call) != crate) continue;
                    if (!RsDocAndAttributeOwnerUtil.existsAfterExpansionSelf(call)) continue;
                    RsMacro[] found = {null};
                    RsMacroCallUtil.processExpansionRecursively(call, expanded -> {
                        if (expanded instanceof RsMacro macro && name.equals(macro.getName()) && RsMacroUtil.getHasMacroExport(macro)) {
                            found[0] = macro;
                            return true;
                        }
                        return false;
                    });
                    if (found[0] != null && Processors.processEntry(processor, name, Namespace.MACROS, found[0])) return true;
                }
            }
        }
        return false;
    }

    @Nonnull
    private static Iterable<Map.Entry<String, List<RsItemElement>>> namedEntries(
        @Nonnull Map<String, List<RsItemElement>> named,
        @Nullable Set<String> names
    ) {
        if (names == null) return named.entrySet();
        Map<String, List<RsItemElement>> result = new LinkedHashMap<>();
        for (String name : names) {
            List<RsItemElement> items = named.get(name);
            if (items != null) result.put(name, items);
        }
        return result.entrySet();
    }

    @Nonnull
    private static List<PsiElement> multiResolve(@Nonnull PsiReference reference) {
        List<PsiElement> result = new ArrayList<>();
        if (reference instanceof PsiPolyVariantReference poly) {
            for (ResolveResult resolveResult : poly.multiResolve(false)) {
                PsiElement element = resolveResult.getElement();
                if (element != null) result.add(element);
            }
        }
        else {
            PsiElement element = reference.resolve();
            if (element != null) result.add(element);
        }
        return result;
    }

    /** A processor that leaves out the names {@code declared} already bound. */
    @Nonnull
    private static RsResolveProcessor notShadowed(@Nonnull RsResolveProcessor processor, @Nonnull Set<String> declared) {
        return Processors.asResolveProcessor(Processors.wrapWithFilter(processor, entry -> !declared.contains(entry.getName())));
    }

    private static boolean isAncestorModule(@Nonnull RsMod module, @Nonnull RsItemsOwner scope) {
        if (!(scope instanceof RsElement element)) return false;
        for (RsMod current = element.getContainingMod(); current != null; current = current.getSuper()) {
            if (current == module) return true;
        }
        return false;
    }

    private static boolean isPublic(@Nonnull RsUseItem useItem) {
        return useItem instanceof RsVisibilityOwner owner && owner.isPublic();
    }

    private static boolean isAtLeastEdition2018(@Nonnull RsItemsOwner scope) {
        if (!(scope instanceof RsElement element)) return true;
        return RsElementUtil.getContainingCrate(element).getEdition().compareTo(CargoWorkspace.Edition.EDITION_2018) >= 0;
    }

    private static boolean isStdlib(@Nonnull Crate crate) {
        return crate.getOrigin() == PackageOrigin.STDLIB || crate.getOrigin() == PackageOrigin.STDLIB_DEPENDENCY;
    }

    private static boolean intersects(@Nonnull Set<Namespace> a, @Nonnull Set<Namespace> b) {
        for (Namespace namespace : a) {
            if (b.contains(namespace)) return true;
        }
        return false;
    }
}
