/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileWithId;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.api.model.CargoProjectsUtil;
import org.rust.cargo.api.workspace.CargoWorkspace;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.crate.CrateGraphService;
import org.rust.lang.core.crate.impl.CargoBasedCrate;
import org.rust.lang.core.crate.impl.DoctestCrate;
import org.rust.lang.core.crate.impl.FakeDetachedCrate;
import org.rust.lang.core.crate.impl.FakeInvalidCrate;
import org.rust.lang.core.macros.*;
import org.rust.lang.core.macros.errors.ResolveMacroWithoutPsiError;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve.*;
import org.rust.lang.core.resolve.ref.ResolveCacheDependency;
import org.rust.lang.core.resolve.ref.RsResolveCache;
import org.rust.openapiext.VirtualFileExtUtil;
import org.rust.stdext.RsResult;

import java.util.*;
import org.rust.lang.core.psi.impl.*;
import org.rust.lang.core.psi.ext.impl.*;

/**
 * The core facade for new name resolution using CrateDefMap.
 */
public final class FacadeResolve {

    private FacadeResolve() {}

    public static boolean processItemDeclarationsInMod(
        @Nonnull RsMod scope,
        @Nonnull Set<Namespace> ns,
        @Nonnull RsResolveProcessor processor,
        boolean withPrivateImports
    ) {
        ItemProcessingMode ipm = withPrivateImports
            ? ItemProcessingMode.WITH_PRIVATE_IMPORTS
            : ItemProcessingMode.WITHOUT_PRIVATE_IMPORTS;
        return processItemDeclarations(scope, ns, processor, ipm);
    }

    public static boolean processItemDeclarations(
        @Nonnull RsItemsOwner scope,
        @Nonnull Set<Namespace> ns,
        @Nonnull RsResolveProcessor processor,
        @Nonnull ItemProcessingMode ipm
    ) {
        RsModInfo info = getModInfo(scope);
        if (info == null) {
            return false;
        }
        return processItemDeclarationsUsingModInfo(scope instanceof RsMod, info, ns, processor, ipm);
    }

    public static boolean processItemDeclarationsUsingModInfo(
        boolean scopeIsMod,
        @Nonnull RsModInfo info,
        @Nonnull Set<Namespace> ns,
        @Nonnull RsResolveProcessor processor,
        @Nonnull ItemProcessingMode ipm
    ) {
        CrateDefMap defMap = info.getDefMap();
        ModData modData = info.getModData();
        Map<RsNamedElement, Set<Namespace>> elements = new HashMap<>();
        for (Map.Entry<String, PerNs> entry : entriesWithNames(modData.getVisibleItems(), processor.getNames()).entrySet()) {
            String name = entry.getKey();
            PerNs perNs = entry.getValue();
            for (Namespace namespace : PerNs.itemNamespaces()) {
                if (!ns.contains(namespace)) continue;
                VisItem[] visItems = perNs.getVisItems(namespace);
                for (VisItem visItem : visItems) {
                    if (ipm == ItemProcessingMode.WITHOUT_PRIVATE_IMPORTS && visItem.getVisibility() == Visibility.INVISIBLE) continue;
                    if (namespace == Namespace.Types && visItem.getVisibility().isInvisible() && defMap.getExternPrelude().containsKey(name)) continue;
                    for (RsNamedElement element : visItem.toPsi(info, namespace)) {
                        Set<Namespace> existing = elements.get(element);
                        if (existing == null) {
                            elements.put(element, EnumSet.of(namespace));
                        } else {
                            existing.add(namespace);
                        }
                    }
                }
            }
            for (Map.Entry<RsNamedElement, Set<Namespace>> elementEntry : elements.entrySet()) {
                RsNamedElement element = elementEntry.getKey();
                Set<Namespace> namespaces = elementEntry.getValue();
                // The name the item is bound to in this scope, not the element's own name: a crate root
                // is an RsFile whose name is "lib.rs", while it is reachable here as e.g. "std".
                if (processor.process(name, namespaces, element)) return true;
            }
            elements.clear();
        }

        if (processor.getNames() == null && ns.contains(Namespace.Types)) {
            for (Map.Entry<ModPath, Visibility> entry : modData.getUnnamedTraitImports().entrySet()) {
                ModPath traitPath = entry.getKey();
                Visibility traitVisibility = entry.getValue();
                VisItem trait = new VisItem(traitPath, traitVisibility);
                for (RsNamedElement traitPsi : trait.toPsi(info, Namespace.Types)) {
                    if (processor.process("_", Namespace.TYPES, traitPsi)) return true;
                }
            }
        }

        java.util.Set<String> wanted = processor.getNames();
        if (wanted != null && wanted.contains("std")) {
        }
        if (ipm.isWithExternCrates() && ns.contains(Namespace.Types) && scopeIsMod) {
            for (Map.Entry<String, CrateDefMap> entry : entriesWithNames(defMap.getExternPrelude(), processor.getNames()).entrySet()) {
                String name = entry.getKey();
                CrateDefMap externCrateDefMap = entry.getValue();
                PerNs existingItemInScope = modData.getVisibleItems().get(name);
                if (existingItemInScope != null) {
                    boolean hasVisible = false;
                    for (VisItem vi : existingItemInScope.getTypes()) {
                        if (!vi.getVisibility().isInvisible()) {
                            hasVisible = true;
                            break;
                        }
                    }
                    if (hasVisible) continue;
                }
                RsMod externCrateRoot = externCrateDefMap.rootAsRsMod(info.getProject());
                if (externCrateRoot == null) continue;
                if (processor.process(name, Namespace.TYPES, externCrateRoot)) return true;
            }
        }

        return false;
    }

    public static boolean processMacros(
        @Nonnull RsItemsOwner scope,
        @Nonnull RsResolveProcessor processor,
        @Nonnull RsPath macroPath
    ) {
        RsModInfo info = getModInfo(scope);
        if (info == null) return false;
        return info.getModData().processMacros(macroPath, processor, info);
    }

    @Nullable
    public static RsNamedElement resolveToMacroUsingNewResolve(@Nonnull RsPossibleMacroCall call) {
        Object[] defAndInfo = resolveToMacroInfo(call);
        if (defAndInfo == null) return null;
        MacroDefInfo defInfo = (MacroDefInfo) defAndInfo[0];
        RsModInfo info = (RsModInfo) defAndInfo[1];
        VisItem visItem = new VisItem(defInfo.getPath(), Visibility.PUBLIC);
        return visItem.scopedMacroToPsi(info);
    }

    @Nonnull
    public static RsResult<RsMacroDataWithHash<?>, ResolveMacroWithoutPsiError> resolveToMacroWithoutPsi(@Nonnull RsMacroCall call) {
        Object[] defAndInfo = resolveToMacroInfo(call);
        if (defAndInfo == null) return new RsResult.Err<>(ResolveMacroWithoutPsiError.Unresolved);
        MacroDefInfo def = (MacroDefInfo) defAndInfo[0];
        return RsMacroDataWithHash.fromDefInfo(def);
    }

    @Nullable
    public static Crate resolveToMacroAndGetContainingCrate(@Nonnull RsMacroCall call) {
        Object[] defAndInfo = resolveToMacroInfo(call);
        if (defAndInfo == null) return null;
        MacroDefInfo def = (MacroDefInfo) defAndInfo[0];
        return CrateGraphService.crateGraph(call.getProject()).findCrateById(def.getCrate());
    }

    @Nullable
    public static Boolean resolveToMacroAndProcessLocalInnerMacros(
        @Nonnull RsMacroCall call,
        @Nonnull RsResolveProcessor processor
    ) {
        Object[] defAndInfo = resolveToMacroInfo(call);
        if (defAndInfo == null) return null;
        MacroDefInfo def = (MacroDefInfo) defAndInfo[0];
        RsModInfo info = (RsModInfo) defAndInfo[1];
        if (!(def instanceof DeclMacroDefInfo) || !((DeclMacroDefInfo) def).isHasLocalInnerMacros()) return null;
        Project project = info.getProject();
        DefMapService service = project.getService(DefMapService.class);
        CrateDefMap defMap = FacadeUpdateDefMapUtil.getOrUpdateIfNeeded(service, def.getCrate());
        if (defMap == null) return null;
        return defMap.getRoot().processMacros(call.getPath(), processor, info);
    }

    @Nullable
    private static Object[] resolveToMacroInfo(@Nonnull RsPossibleMacroCall call) {
        RsItemsOwner scope = RsElementUtil.contextStrict(call, RsItemsOwner.class);
        if (scope == null) return null;
        RsModInfo info = HangingModData.getNearestAncestorModInfo(scope);
        if (info == null) return null;
        MacroDefInfo def;
        RsPossibleMacroCallKind kind = RsPossibleMacroCallUtil.getKind(call);
        if (kind instanceof RsPossibleMacroCallKind.MacroCall) {
            def = resolveToMacroDefInfo(((RsPossibleMacroCallKind.MacroCall) kind).call, info);
        } else if (kind instanceof RsPossibleMacroCallKind.MetaItem) {
            def = resolveToProcMacroWithoutPsi(((RsPossibleMacroCallKind.MetaItem) kind).meta, true);
        } else {
            return null;
        }
        if (def == null) return null;
        return new Object[]{def, info};
    }

    @Nullable
    public static ProcMacroDefInfo resolveToProcMacroWithoutPsi(
        @Nonnull RsMetaItem metaItem,
        boolean checkIsMacroAttr
    ) {
        RsAttrProcMacroOwner owner = RsMetaItemExtUtil.getOwner(metaItem) instanceof RsAttrProcMacroOwner
            ? (RsAttrProcMacroOwner) RsMetaItemExtUtil.getOwner(metaItem) : null;
        if (owner == null) return null;
        if (!RsProcMacroPsiUtil.canBeProcMacroCall(metaItem)) return null;
        RsModInfo info = getModInfo(owner.getContainingMod());
        if (info == null) return null;
        if (checkIsMacroAttr && !RsPossibleMacroCallUtil.isMacroCall(metaItem)) return null;
        PsiElement context = owner.getContext();
        if (context == null || !RsElementUtil.existsAfterExpansion(context)) return null;

        CrateDefMap defMap = info.getDefMap();
        ModData modData = info.getModData();
        RsPath path = metaItem.getPath();
        String[] macroPath = path != null ? RsPathUtil.getPathSegmentsAdjustedForAttrMacro(path) : null;
        if (macroPath == null) return null;
        if (macroPath.length == 1) {
            String name = macroPath[0];
            List<MacroDefInfo> legacyMacros = modData.getLegacyMacros().get(name);
            if (legacyMacros != null) {
                for (int i = legacyMacros.size() - 1; i >= 0; i--) {
                    MacroDefInfo m = legacyMacros.get(i);
                    if (m instanceof ProcMacroDefInfo) return (ProcMacroDefInfo) m;
                }
            }
        }
        PathResolution.ResolvePathResult resolveResult = PathResolution.resolvePathFp(defMap, modData, macroPath, ResolveMode.OTHER, false);
        VisItem[] macros = resolveResult.getResolvedDef().getMacros();
        if (macros.length != 1) return null;
        MacroDefInfo macroInfo = defMap.getMacroInfo(macros[0]);
        return macroInfo instanceof ProcMacroDefInfo ? (ProcMacroDefInfo) macroInfo : null;
    }

    @Nullable
    private static MacroDefInfo resolveToMacroDefInfo(@Nonnull RsMacroCall call, @Nonnull RsModInfo containingModInfo) {
        Project project = containingModInfo.getProject();
        CrateDefMap defMap = containingModInfo.getDefMap();
        ModData modData = containingModInfo.getModData();
        Crate crate = containingModInfo.getCrate();
        return RsResolveCache.getInstance(project).resolveWithCaching(call, ResolveCacheDependency.LOCAL_AND_RUST_STRUCTURE, (c) -> {
            RsPath callPath = c.getPath();
            String[] pathSegments = callPath != null ? RsPathUtil.getPathSegmentsAdjusted(callPath) : null;
            if (pathSegments == null) return null;
            MacroIndex macroIndex = containingModInfo.getMacroIndex(c, crate);
            if (macroIndex == null) return null;
            MacroDefInfo result = PathResolution.resolveMacroCallToMacroDefInfo(defMap, modData, pathSegments, macroIndex);
            if (result instanceof ProcMacroDefInfo && ((ProcMacroDefInfo) result).getProcMacroKind() != RsProcMacroKind.FUNCTION_LIKE) {
                return null;
            }
            return result;
        });
    }

    @Nullable
    public static RsModInfo getModInfo(@Nonnull RsItemsOwner scope0) {
        RsItemsOwner scope = scope0.getOriginalElement() instanceof RsItemsOwner
            ? (RsItemsOwner) scope0.getOriginalElement() : scope0;
        if (!(scope instanceof RsMod)) return HangingModData.getHangingModInfo(scope);
        Project project = scope.getProject();
        if (scope instanceof RsModItem) {
            if (TMP_MOD_NAME.equals(((RsModItem) scope).getModName())) {
                return HangingModData.getTmpModInfo((RsModItem) scope);
            }
            if (isLocalMod((RsMod) scope)) {
                return HangingModData.getLocalModInfo((RsMod) scope);
            }
        }
        Crate crate = RsElementUtil.getContainingCrate((RsElement) scope);
        if (crate instanceof CargoBasedCrate) {
            // normal crate
        } else if (crate instanceof DoctestCrate) {
            return DetachedDefMap.getDoctestModInfo(project, (RsMod) scope, (DoctestCrate) crate);
        } else if (crate instanceof FakeDetachedCrate) {
            return DetachedDefMap.getDetachedModInfo(project, (RsMod) scope, (FakeDetachedCrate) crate);
        } else if (crate instanceof FakeInvalidCrate) {
            return null;
        } else {
            throw new IllegalStateException("unreachable");
        }

        CrateDefMap defMap = getDefMap(project, crate);
        if (defMap == null) return null;
        ModData modData = defMap.getModData((RsMod) scope);
        if (modData == null) return null;

        if (isModShadowedByOtherMod((RsMod) scope, modData, crate)) {
            ModData parent = modData.getParent();
            if (parent == null) return null;
            RsModInfo contextInfo = new RsModInfo(project, defMap, parent, crate, null);
            return HangingModData.getHangingModInfo(scope, contextInfo);
        }

        return new RsModInfo(project, defMap, modData, crate, null);
    }

    @Nullable
    private static CrateDefMap getDefMap(@Nonnull Project project, @Nonnull Crate crate) {
        if (crate instanceof DoctestCrate) throw new IllegalStateException("doc test crates are not supported by CrateDefMap");
        Integer crateId = crate.getId();
        if (crateId == null) return null;
        DefMapService service = project.getService(DefMapService.class);
        CrateDefMap defMap = FacadeUpdateDefMapUtil.getOrUpdateIfNeeded(service, crateId);
        if (defMap == null) {
            CrateDefMap.RESOLVE_LOG.warn("DefMap is null for " + crate + " during resolve");
        }
        return defMap;
    }

    private static boolean isLocalMod(@Nonnull RsMod mod) {
        return RsElementUtil.ancestorStrict(mod, RsBlock.class) != null;
    }

    private static boolean isModShadowedByOtherMod(@Nonnull RsMod mod, @Nonnull ModData modData, @Nonnull Crate crate) {
        if (mod instanceof RsFile) {
            return modData.isShadowedByOtherFile();
        } else {
            RsFile containingFile = (RsFile) mod.getContainingFile();
            boolean isDeeplyEnabledByCfg = containingFile.isDeeplyEnabledByCfg() && RsElementUtil.isEnabledByCfg((PsiElement) mod, crate);
            return isDeeplyEnabledByCfg != modData.isDeeplyEnabledByCfg();
        }
    }

    @Nonnull
    public static <T> Map<String, T> entriesWithNames(@Nonnull Map<String, T> map, @Nullable Set<String> names) {
        if (names == null || names.isEmpty()) return map;
        if (names.size() == 1) {
            String single = names.iterator().next();
            T value = map.get(single);
            if (value == null) return Collections.emptyMap();
            return Collections.singletonMap(single, value);
        }
        Map<String, T> result = new HashMap<>();
        for (String name : names) {
            T value = map.get(name);
            if (value != null) result.put(name, value);
        }
        return result;
    }

    /**
     * Finds file inclusion points for a given RsFile.
     */
    @Nonnull
    public static List<FileInclusionPoint> findFileInclusionPointsFor(@Nonnull RsFile file) {
        Project project = file.getProject();
        DefMapService defMapService = project.getService(DefMapService.class);
        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile == null) return Collections.emptyList();
        if (!(virtualFile instanceof VirtualFileWithId)) return Collections.emptyList();

        if (!defMapService.areAllDefMapsUpToDate()) {
            CargoWorkspace.Package pkg = CargoProjectsUtil.findPackageForFile(project, virtualFile);
            if (pkg != null) {
                List<Integer> crateIds = new ArrayList<>();
                for (CargoWorkspace.Target target : pkg.getTargets()) {
                    VirtualFile crateRoot = target.getCrateRoot();
                    if (crateRoot == null) continue;
                    Crate c = CrateGraphService.crateGraph(project).findCrateByRootMod(crateRoot);
                    if (c != null && c.getId() != null) crateIds.add(c.getId());
                }
                FacadeUpdateDefMap.getOrUpdateIfNeeded(defMapService, crateIds);
            } else {
                NameResolutionTestmarks.UpdateDefMapsForAllCratesWhenFindingModData.hit();
                FacadeUpdateDefMap.updateDefMapForAllCrates(defMapService);
            }
        }

        Collection<Integer> crateIds = defMapService.findCrates(file);
        boolean trace = file.getName().equals("main.rs");
        if (trace) {
        }
        List<FileInclusionPoint> rawList = new ArrayList<>();
        for (int crateId : crateIds) {
            CrateDefMap defMap = FacadeUpdateDefMapUtil.getOrUpdateIfNeeded(defMapService, crateId);
            if (defMap == null) continue;
            FileInfo fileInfo = defMap.getFileInfos().get(VirtualFileExtUtil.getFileId(virtualFile));
            if (trace) {
            }
            if (fileInfo == null) continue;
            rawList.add(new FileInclusionPoint(defMap, fileInfo.getModData(), fileInfo.getIncludeMacroIndex()));
        }

        if (rawList.size() <= 1) return rawList;
        List<FileInclusionPoint> filtered = new ArrayList<>();
        for (FileInclusionPoint point : rawList) {
            if (point.getModData().isDeeplyEnabledByCfg()) filtered.add(point);
        }
        return filtered.isEmpty() ? rawList : filtered;
    }

    private static final String TMP_MOD_NAME = "__tmp__";

    /**
     * Represents a file inclusion point in a CrateDefMap.
     */
    public static final class FileInclusionPoint {
        @Nonnull private final CrateDefMap defMap;
        @Nonnull private final ModData modData;
        @Nullable private final MacroIndex includeMacroIndex;

        public FileInclusionPoint(
            @Nonnull CrateDefMap defMap,
            @Nonnull ModData modData,
            @Nullable MacroIndex includeMacroIndex
        ) {
            this.defMap = defMap;
            this.modData = modData;
            this.includeMacroIndex = includeMacroIndex;
        }

        @Nonnull public CrateDefMap getDefMap() { return defMap; }
        @Nonnull public ModData getModData() { return modData; }
        @Nullable public MacroIndex getIncludeMacroIndex() { return includeMacroIndex; }
    }
}
