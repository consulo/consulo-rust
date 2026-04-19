/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import com.intellij.concurrency.SensitiveProgressWrapper;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.newvfs.persistent.PersistentFS;
import com.intellij.psi.stubs.StubElement;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.macros.*;
import org.rust.lang.core.macros.proc.ProcMacroApplicationService;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsItemsOwner;
import org.rust.lang.core.resolve.Namespace;
import org.rust.lang.core.resolve2.util.DollarCrateMap;
import org.rust.lang.core.resolve2.util.DollarCrateUtil;
// import org.rust.lang.core.stubs.RsBlockStubBuilderUtil; // placeholder removed
import org.rust.openapiext.OpenApiUtil;
import org.rust.stdext.HashCode;
import org.rust.stdext.CollectionsUtil;

import java.nio.file.InvalidPathException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

/**
 * Resolves all imports and expands macros (new items are added to defMap) using fixed point iteration algorithm.
 */
public class DefCollector {
    private static final boolean CONSIDER_INDETERMINATE_IMPORTS_AS_RESOLVED = false;

    @NotNull
    private final Project project;
    @NotNull
    private final CrateDefMap defMap;
    @NotNull
    private final CollectorContext context;
    @Nullable
    private final ExecutorService pool;
    @NotNull
    private final ProgressIndicator indicator;

    @NotNull
    private final Map<ModData, Map<ModData, Visibility>> globImports = new HashMap<>();
    @NotNull
    private final List<Import> unresolvedImports;
    @NotNull
    private final List<Import> resolvedImports = new ArrayList<>();
    @NotNull
    private final List<MacroCallInfoBase> macroCallsToExpand;

    private final FunctionLikeMacroExpander macroExpander;
    private final MacroExpansionSharedCache macroExpanderShared;
    @NotNull
    private final Map<HashCode, Integer> macroMixHashToOrder = new THashMap<>();

    private final boolean shouldExpandMacros;
    private final int recursionLimit;

    public DefCollector(
        @NotNull Project project,
        @NotNull CrateDefMap defMap,
        @NotNull CollectorContext context,
        @Nullable ExecutorService pool,
        @NotNull ProgressIndicator indicator
    ) {
        this.project = project;
        this.defMap = defMap;
        this.context = context;
        this.pool = pool;
        this.indicator = indicator;
        this.unresolvedImports = context.getImports();
        this.macroCallsToExpand = context.getMacroCalls();
        this.macroExpander = FunctionLikeMacroExpander.forCrate(context.getCrate());
        this.macroExpanderShared = MacroExpansionSharedCache.getInstance();

        MacroExpansionMode mode = project.getService(org.rust.lang.core.macros.MacroExpansionManager.class).getMacroExpansionMode();
        if (mode instanceof MacroExpansionMode.Disabled) {
            shouldExpandMacros = false;
        } else if (mode instanceof MacroExpansionMode.New) {
            shouldExpandMacros = ((MacroExpansionMode.New) mode).getScope() != MacroExpansionScope.NONE;
        } else {
            shouldExpandMacros = true;
        }

        this.recursionLimit = defMap.getRecursionLimit();
    }

    public void collect() {
        do {
            removeInvalidImportsAndMacroCalls(defMap, context);
            sortImports(unresolvedImports);
            resolveImports();
            boolean changed = expandMacros();
            if (!changed) break;
        } while (true);
        if (!context.isHangingMode()) {
            afterBuilt(defMap);
        }
    }

    private void resolveImports() {
        boolean hasResolvedImports;
        do {
            final boolean[] hasChangedIndeterminateImports = {false};
            hasResolvedImports = unresolvedImports.removeIf(imp -> {
                ProgressManager.checkCanceled();
                PartialResolvedImport status = resolveImport(imp);
                if (status instanceof PartialResolvedImport.Indeterminate) {
                    if (imp.getStatus() instanceof PartialResolvedImport.Indeterminate
                        && imp.getStatus().equals(status)) {
                        return false;
                    }
                    imp.setStatus(status);
                    boolean changed = recordResolvedImport(imp);
                    if (changed) hasChangedIndeterminateImports[0] = true;
                    if (CONSIDER_INDETERMINATE_IMPORTS_AS_RESOLVED) {
                        resolvedImports.add(imp);
                        return true;
                    } else {
                        return false;
                    }
                } else if (status instanceof PartialResolvedImport.Resolved) {
                    imp.setStatus(status);
                    recordResolvedImport(imp);
                    resolvedImports.add(imp);
                    return true;
                } else {
                    return false;
                }
            });
        } while (hasResolvedImports || false);
    }

    @NotNull
    private PartialResolvedImport resolveImport(@NotNull Import imp) {
        if (imp.isExternCrate()) {
            String externCrateName = imp.getUsePath()[0];
            CrateDefMap externCrateDefMap = PathResolutionUtil.resolveExternCrateAsDefMap(defMap, externCrateName);
            if (externCrateDefMap == null) return PartialResolvedImport.Unresolved.INSTANCE;
            PerNs def = externCrateDefMap.getRootAsPerNs().adjust(imp.getVisibility(), true);
            return new PartialResolvedImport.Resolved(def);
        }

        PathResolution.ResolvePathResult result = PathResolution.resolvePathFp(
            defMap,
            imp.getContainingMod(),
            imp.getUsePath(),
            ResolveMode.IMPORT,
            imp.getVisibility().isInvisible()
        );
        PerNs perNs = result.getResolvedDef();

        if (!result.isReachedFixedPoint() || perNs.isEmpty()) return PartialResolvedImport.Unresolved.INSTANCE;

        if (result.isVisitedOtherCrate()) return new PartialResolvedImport.Resolved(perNs);

        boolean isResolvedInAllNamespaces = perNs.hasAllNamespaces();
        boolean isResolvedGlobImport = imp.isGlob() && perNs.getTypes().length > 0;
        if (isResolvedInAllNamespaces || isResolvedGlobImport) {
            return new PartialResolvedImport.Resolved(perNs);
        } else {
            return new PartialResolvedImport.Indeterminate(perNs);
        }
    }

    private boolean recordResolvedImport(@NotNull Import imp) {
        PerNs def;
        PartialResolvedImport status = imp.getStatus();
        if (status instanceof PartialResolvedImport.Resolved) {
            def = ((PartialResolvedImport.Resolved) status).getPerNs();
        } else if (status instanceof PartialResolvedImport.Indeterminate) {
            def = ((PartialResolvedImport.Indeterminate) status).getPerNs();
        } else {
            throw new IllegalStateException("expected resolved import");
        }

        if (imp.isGlob()) {
            return recordResolvedGlobImport(imp, def);
        } else {
            return recordResolvedNamedImport(imp, def);
        }
    }

    private boolean recordResolvedGlobImport(@NotNull Import imp, @NotNull PerNs def) {
        VisItem types = null;
        for (VisItem t : def.getTypes()) {
            if (t.isModOrEnum()) {
                types = t;
                break;
            }
        }
        if (types == null) return false;

        ModData targetMod = defMap.tryCastToModData(types, context.getHangingModData());
        if (targetMod == null) return false;
        ModData containingMod = imp.getContainingMod();

        if (imp.isPrelude()) {
            defMap.setPrelude(targetMod);
            return true;
        } else if (targetMod.getCrate() == defMap.getCrate()) {
            List<Map.Entry<String, PerNs>> items = targetMod.getVisibleItems(v -> v.isVisibleFromMod(containingMod));
            boolean changed = update(containingMod, items, imp.getVisibility(), ImportType.GLOB);

            if (!context.isHangingMode()) {
                defMap.getGlobImportGraph().recordGlobImport(containingMod, targetMod, imp.getVisibility());
            }

            Map<ModData, Visibility> globImportsMap = globImports.computeIfAbsent(targetMod, k -> new HashMap<>());
            Visibility existingVis = globImportsMap.get(containingMod);
            if (existingVis == null || !existingVis.isStrictlyMorePermissive(imp.getVisibility())) {
                globImportsMap.put(containingMod, imp.getVisibility());
            }
            return changed;
        } else {
            List<Map.Entry<String, PerNs>> items = targetMod.getVisibleItems(Visibility::isVisibleFromOtherCrate);
            return update(containingMod, items, imp.getVisibility(), ImportType.GLOB);
        }
    }

    private boolean recordResolvedNamedImport(@NotNull Import imp, @NotNull PerNs def) {
        ModData containingMod = imp.getContainingMod();
        String name = imp.getNameInScope();

        if (imp.isExternCrate() && containingMod.isCrateRoot() && !name.equals("_") && !context.isHangingMode()) {
            VisItem typesItem = def.getTypes().length == 1 ? def.getTypes()[0] : null;
            if (typesItem != null) {
                CrateDefMap externCrateDefMap = defMap.getDefMap(typesItem.getPath().getCrate());
                if (externCrateDefMap != null) {
                    defMap.getExternPrelude().put(name, externCrateDefMap);
                    defMap.getExternCratesInRoot().put(name, externCrateDefMap);
                }
            }
        }

        PerNs defWithAdjustedVisible = def.mapItems(it -> {
            if (it.getVisibility().isInvisible() || it.getVisibility().isVisibleFromMod(containingMod)) {
                return it;
            } else {
                return it.copy(Visibility.INVISIBLE, it.isFromNamedImport());
            }
        });

        List<Map.Entry<String, PerNs>> items = new ArrayList<>();
        items.add(new AbstractMap.SimpleEntry<>(name, defWithAdjustedVisible));
        return update(containingMod, items, imp.getVisibility(), ImportType.NAMED);
    }

    private boolean update(
        @NotNull ModData modData,
        @NotNull List<Map.Entry<String, PerNs>> resolutions,
        @NotNull Visibility visibility,
        @NotNull ImportType importType
    ) {
        return updateRecursive(modData, resolutions, visibility, importType, 0);
    }

    private boolean updateRecursive(
        @NotNull ModData modData,
        @NotNull List<Map.Entry<String, PerNs>> resolutions,
        @NotNull Visibility visibility,
        @NotNull ImportType importType,
        int depth
    ) {
        if (depth > 100) throw new IllegalStateException("infinite recursion in glob imports!");

        List<Map.Entry<String, PerNs>> resolutionsNew = new ArrayList<>();
        for (Map.Entry<String, PerNs> entry : resolutions) {
            String name = entry.getKey();
            PerNs def = entry.getValue();
            boolean pushed;
            if (!name.equals("_")) {
                PerNs defAdjusted = def.adjust(visibility, importType == ImportType.NAMED).adjustMultiresolve();
                pushed = DefCollectorUtil.pushResolutionFromImport(modData, name, defAdjusted);
            } else {
                pushed = pushTraitResolutionFromImport(modData, def, visibility);
            }
            if (pushed) {
                resolutionsNew.add(entry);
            }
        }

        boolean changed = !resolutionsNew.isEmpty();
        if (!changed) return false;

        Map<ModData, Visibility> globImportsForMod = globImports.get(modData);
        if (globImportsForMod == null) return true;

        for (Map.Entry<ModData, Visibility> entry : globImportsForMod.entrySet()) {
            ModData globImportingMod = entry.getKey();
            Visibility globImportVis = entry.getValue();
            if (!visibility.isVisibleFromMod(globImportingMod)) continue;
            updateRecursive(globImportingMod, resolutionsNew, globImportVis, ImportType.GLOB, depth + 1);
        }
        return true;
    }

    private boolean pushTraitResolutionFromImport(
        @NotNull ModData modData,
        @NotNull PerNs def,
        @NotNull Visibility visibility
    ) {
        if (def.isEmpty()) throw new IllegalStateException("def is empty");
        boolean changed = false;
        for (VisItem trait : def.getTypes()) {
            if (trait.isModOrEnum()) continue;
            Visibility oldVisibility = modData.getUnnamedTraitImports().get(trait.getPath());
            if (oldVisibility == null || visibility.isStrictlyMorePermissive(oldVisibility)) {
                modData.getUnnamedTraitImports().put(trait.getPath(), visibility);
                changed = true;
            }
        }
        return changed;
    }

    private boolean expandMacros() {
        if (!shouldExpandMacros) return false;
        // Simplified: actual expansion logic is complex
        return false;
    }

    private static void removeInvalidImportsAndMacroCalls(
        @NotNull CrateDefMap defMap,
        @NotNull CollectorContext context
    ) {
        if (context.isHangingMode()) return;
        Set<ModData> allMods = new HashSet<>();
        collectChildMods(defMap.getRoot(), allMods);
        context.getImports().removeIf(imp -> !allMods.contains(imp.getContainingMod()));
        context.getMacroCalls().removeIf(call -> !allMods.contains(call.getContainingMod()));
    }

    private static void collectChildMods(@NotNull ModData mod, @NotNull Set<ModData> allMods) {
        allMods.add(mod);
        for (ModData child : mod.getChildModules().values()) {
            collectChildMods(child, allMods);
        }
    }

    private static void sortImports(@NotNull List<Import> imports) {
        imports.sort(Comparator.<Import, Boolean>comparing(i -> i.getVisibility() == Visibility.CFG_DISABLED)
            .thenComparing(Import::isGlob)
            .thenComparing(Comparator.<Import, Boolean>comparing(i -> i.getContainingMod().getVisibleItems().containsKey(i.getNameInScope())).reversed())
            .thenComparing(Comparator.<Import, Integer>comparing(i -> i.getContainingMod().getPath().getSegments().length).reversed())
        );
    }

    private static void afterBuilt(@NotNull CrateDefMap defMap) {
        defMap.getRoot().visitDescendants(mod -> mod.setShadowedByOtherFile(false));
    }
}
