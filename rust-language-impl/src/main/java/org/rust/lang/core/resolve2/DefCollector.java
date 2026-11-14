/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.macros.*;
import org.rust.lang.core.psi.*;
// import org.rust.lang.core.stubs.RsBlockStubBuilderUtil; // placeholder removed
import org.rust.stdext.HashCode;
import org.rust.stdext.RsResult;
import org.rust.lang.core.psi.impl.RsFile;
import org.rust.lang.core.psi.impl.RsPsiFactory;
import org.rust.lang.core.psi.ext.RsItemsOwner;

import java.util.*;
import java.util.concurrent.ExecutorService;
import org.rust.lang.core.macros.MacroExpansionManager;

/**
 * Resolves all imports and expands macros (new items are added to defMap) using fixed point iteration algorithm.
 */
public class DefCollector {
    private static final boolean CONSIDER_INDETERMINATE_IMPORTS_AS_RESOLVED = false;

    @Nonnull
    private final Project project;
    @Nonnull
    private final CrateDefMap defMap;
    @Nonnull
    private final CollectorContext context;
    @Nullable
    private final ExecutorService pool;
    @Nonnull
    private final ProgressIndicator indicator;

    @Nonnull
    private final Map<ModData, Map<ModData, Visibility>> globImports = new HashMap<>();
    @Nonnull
    private final List<Import> unresolvedImports;
    @Nonnull
    private final List<Import> resolvedImports = new ArrayList<>();
    @Nonnull
    private final List<MacroCallInfoBase> macroCallsToExpand;

    private final FunctionLikeMacroExpander macroExpander;
    @Nonnull
    private final Map<HashCode, Integer> macroMixHashToOrder = new HashMap<>();

    private final boolean shouldExpandMacros;
    private final int recursionLimit;

    public DefCollector(
        @Nonnull Project project,
        @Nonnull CrateDefMap defMap,
        @Nonnull CollectorContext context,
        @Nullable ExecutorService pool,
        @Nonnull ProgressIndicator indicator
    ) {
        this.project = project;
        this.defMap = defMap;
        this.context = context;
        this.pool = pool;
        this.indicator = indicator;
        this.unresolvedImports = context.getImports();
        this.macroCallsToExpand = context.getMacroCalls();
        this.macroExpander = FunctionLikeMacroExpander.forCrate(context.getCrate());

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

    @Nonnull
    private PartialResolvedImport resolveImport(@Nonnull Import imp) {
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

    private boolean recordResolvedImport(@Nonnull Import imp) {
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

    private boolean recordResolvedGlobImport(@Nonnull Import imp, @Nonnull PerNs def) {
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

    private boolean recordResolvedNamedImport(@Nonnull Import imp, @Nonnull PerNs def) {
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
        @Nonnull ModData modData,
        @Nonnull List<Map.Entry<String, PerNs>> resolutions,
        @Nonnull Visibility visibility,
        @Nonnull ImportType importType
    ) {
        return updateRecursive(modData, resolutions, visibility, importType, 0);
    }

    private boolean updateRecursive(
        @Nonnull ModData modData,
        @Nonnull List<Map.Entry<String, PerNs>> resolutions,
        @Nonnull Visibility visibility,
        @Nonnull ImportType importType,
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
        @Nonnull ModData modData,
        @Nonnull PerNs def,
        @Nonnull Visibility visibility
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

    /**
     * Expands the macro calls whose macro is resolved by now and collects the items they expand to, so that the
     * next round of import resolution sees them. Expansions are parsed in memory; nothing is written to disk.
     * Returns whether any call was consumed.
     */
    private boolean expandMacros() {
        if (!shouldExpandMacros) return false;
        List<MacroCallInfo> resolvedCalls = new ArrayList<>();
        List<MacroDefInfo> resolvedDefs = new ArrayList<>();
        boolean changed = false;
        // new calls found in the expansions are added to the same list, so the list is drained before expanding
        for (Iterator<MacroCallInfoBase> iterator = macroCallsToExpand.iterator(); iterator.hasNext(); ) {
            ProgressManager.checkCanceled();
            MacroCallInfoBase call = iterator.next();
            if (call instanceof MacroCallInfo macroCall) {
                String[] path = macroCall.getPath();
                if ("include".equals(path[path.length - 1])) {
                    iterator.remove();
                    expandIncludeMacroCall(macroCall);
                    changed = true;
                    continue;
                }
                MacroDefInfo def = PathResolution.resolveMacroCallToMacroDefInfo(
                    defMap, macroCall.getContainingMod(), path, macroCall.getMacroIndex());
                if (def == null || def.getProcMacroKind() != org.rust.lang.core.psi.impl.RsProcMacroKind.FUNCTION_LIKE) continue;
                iterator.remove();
                resolvedCalls.add(macroCall);
                resolvedDefs.add(def);
                changed = true;
            }
            else if (call instanceof ProcMacroCallInfo procMacroCall) {
                // attribute and derive macros are not expanded: the item they are attached to stays as written
                iterator.remove();
                treatAsIdentityMacro(procMacroCall);
                changed = true;
            }
        }
        for (int i = 0; i < resolvedCalls.size(); i++) {
            ProgressManager.checkCanceled();
            expandMacro(resolvedCalls.get(i), resolvedDefs.get(i));
        }
        return changed;
    }

    private void expandMacro(@Nonnull MacroCallInfo call, @Nonnull MacroDefInfo def) {
        RsResult<RsMacroDataWithHash<?>, ?> defData = RsMacroDataWithHash.fromDefInfo(def);
        if (defData.isErr()) return;
        RsMacroData data = defData.ok().getData();
        // function-like procedural macros need the expander process, which is not run while building the def map
        if (data instanceof RsProcMacroData) return;

        RsMacroCallData callData = new RsMacroCallData(call.getBody(), context.getCrate().getEnv());
        RsResult<consulo.util.lang.Pair<CharSequence, RangeMap>, ?> expanded = macroExpander.expandMacroAsTextWithErr(data, callData);
        if (expanded.isErr()) return;
        String text = expanded.ok().getFirst().toString();
        ExpansionResultOk expansion = new ExpansionResultOk(text, expanded.ok().getSecond());

        consulo.language.psi.stub.StubElement<? extends RsItemsOwner> expandedScope;
        RsPsiFactory factory = new RsPsiFactory(project, false);
        if (call.getContainingMod().isBlock()) {
            // a macro called in a block body is expanded in statement context
            MacroExpansionContext stmt = MacroExpansionContext.STMT;
            if (!(factory.createPsiFile(stmt.prepareExpandedTextForParsing(text)) instanceof RsFile file)) return;
            RsBlock block = org.rust.lang.core.psi.ext.impl.RsElementUtil.stubDescendantOfTypeOrStrict(file, RsBlock.class);
            if (block == null) return;
            expandedScope = org.rust.lang.core.resolve2.util.RsBlockStubBuilder.buildStub(block);
            expansion = expansion.shiftDstRangesRight(stmt.getExpansionFileStartOffset());
        }
        else {
            if (!(factory.createPsiFile(text) instanceof RsFile file)) return;
            expandedScope = ModCollector.getOrBuildFileStub(file);
        }
        if (expandedScope == null) return;

        ModCollectorContext collectorContext = getModCollectorContextForExpandedElements(call);
        if (collectorContext == null) return;
        org.rust.lang.core.resolve2.util.DollarCrateHelper dollarCrateHelper =
            org.rust.lang.core.resolve2.util.DollarCrateUtil.createDollarCrateHelper(call, def, expansion);
        ModCollector.collectExpandedElements(expandedScope, call, collectorContext, dollarCrateHelper);
    }

    /** `include!("file.rs")`: the items of the file, as if written in place of the call. */
    private void expandIncludeMacroCall(@Nonnull MacroCallInfo call) {
        Integer containingFileId = call.getContainingFileId();
        if (containingFileId == null) return;
        consulo.virtualFileSystem.VirtualFile containingFile =
            consulo.virtualFileSystem.ManagingFS.getInstance().findFileById(containingFileId);
        if (containingFile == null || !(call.getBody() instanceof MacroCallBody.FunctionLike functionLike)) return;
        String includePath = functionLike.getText();
        consulo.virtualFileSystem.VirtualFile parentDirectory = containingFile.getParent();
        if (parentDirectory == null) return;
        // an absolute path is a path of the same machine as the including file
        java.nio.file.Path parentPath;
        try {
            parentPath = parentDirectory.toNioPath();
        }
        catch (UnsupportedOperationException e) {
            parentPath = null;
        }
        consulo.virtualFileSystem.VirtualFile includingFile = parentPath != null
            ? consulo.virtualFileSystem.VirtualFileManager.getInstance().findFileByNioPath(parentPath.resolve(includePath))
            : parentDirectory.findFileByRelativePath(includePath);
        if (includingFile == null) {
            if (!context.isHangingMode() && parentPath != null) {
                try {
                    defMap.getMissedFiles().add(parentPath.resolve(includePath));
                }
                catch (java.nio.file.InvalidPathException ignored) {
                }
            }
            return;
        }
        if (!(consulo.language.psi.PsiManager.getInstance(project).findFile(includingFile) instanceof RsFile includingRsFile)) return;
        ModCollectorContext collectorContext = getModCollectorContextForExpandedElements(call);
        if (collectorContext == null) return;
        ModCollector.collectScope(
            includingRsFile,
            call.getContainingMod(),
            collectorContext,
            call.getMacroIndex(),
            null,
            call.getMacroIndex(),
            true
        );
        ModCollector.recordChildFileInUnusualLocation(call.getContainingMod(), org.rust.openapiext.VirtualFileExtUtil.getFileId(includingFile));
    }

    @Nullable
    private ModCollectorContext getModCollectorContextForExpandedElements(@Nonnull MacroCallInfo call) {
        if (call.getDepth() >= recursionLimit) return null;
        return new ModCollectorContext(defMap, context, call.getDepth() + 1, this::onAddItem);
    }

    private boolean onAddItem(@Nonnull ModData modData, @Nonnull String name, @Nonnull PerNs perNs, @Nonnull Visibility visibility) {
        return update(modData, Collections.singletonList(new AbstractMap.SimpleEntry<>(name, perNs)), visibility, ImportType.NAMED);
    }

    /** The item an attribute macro is attached to, added as written - see {@code ModCollector.collectSimpleItem}. */
    private void treatAsIdentityMacro(@Nonnull ProcMacroCallInfo call) {
        org.rust.lang.core.resolve.SelectionCandidate.Triple<VisItem, Set<org.rust.lang.core.resolve.Namespace>, org.rust.lang.core.psi.impl.RsProcMacroKind> originalItem =
            call.getOriginalItem();
        if (originalItem == null) return;
        VisItem visItem = originalItem.getFirst();
        PerNs perNs = PerNs.from(visItem, originalItem.getSecond());
        update(call.getContainingMod(), Collections.singletonList(new AbstractMap.SimpleEntry<>(visItem.getName(), perNs)), visItem.getVisibility(), ImportType.NAMED);
        if (originalItem.getThird() != null) {
            call.getContainingMod().getProcMacros().put(visItem.getName(), originalItem.getThird());
        }
    }

    private static void removeInvalidImportsAndMacroCalls(
        @Nonnull CrateDefMap defMap,
        @Nonnull CollectorContext context
    ) {
        if (context.isHangingMode()) return;
        Set<ModData> allMods = new HashSet<>();
        collectChildMods(defMap.getRoot(), allMods);
        context.getImports().removeIf(imp -> !allMods.contains(imp.getContainingMod()));
        context.getMacroCalls().removeIf(call -> !allMods.contains(call.getContainingMod()));
    }

    private static void collectChildMods(@Nonnull ModData mod, @Nonnull Set<ModData> allMods) {
        allMods.add(mod);
        for (ModData child : mod.getChildModules().values()) {
            collectChildMods(child, allMods);
        }
    }

    private static void sortImports(@Nonnull List<Import> imports) {
        imports.sort(Comparator.<Import, Boolean>comparing(i -> i.getVisibility() == Visibility.CFG_DISABLED)
            .thenComparing(Import::isGlob)
            .thenComparing(Comparator.<Import, Boolean>comparing(i -> i.getContainingMod().getVisibleItems().containsKey(i.getNameInScope())).reversed())
            .thenComparing(Comparator.<Import, Integer>comparing(i -> i.getContainingMod().getPath().getSegments().length).reversed())
        );
    }

    private static void afterBuilt(@Nonnull CrateDefMap defMap) {
        defMap.getRoot().visitDescendants(mod -> mod.setShadowedByOtherFile(false));
    }
}
