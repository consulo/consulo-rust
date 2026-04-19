/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.cargo.util.AutoInjectedCrates;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.crate.CratePersistentId;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.RsPsiUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.openapiext.OpenApiUtil;
import org.rust.openapiext.VirtualFileExtUtil;

import java.util.*;
import java.util.concurrent.ExecutorService;

/**
 * Provides functions for building CrateDefMap.
 */
public final class FacadeBuildDefMap {

    private FacadeBuildDefMap() {}

    /**
     * Returns {@code null} if crate has null id or rootMod,
     * or if crate should not be indexed (e.g. test/bench non-workspace crate).
     */
    @Nullable
    public static CrateDefMap buildDefMap(
        @NotNull Crate crate,
        @NotNull Map<Crate, CrateDefMap> allDependenciesDefMaps,
        @Nullable ExecutorService pool,
        @NotNull ProgressIndicator indicator,
        boolean isNormalCrate
    ) {
        OpenApiUtil.checkReadAccessAllowed();
        Project project = crate.getProject();
        CollectorContext context = new CollectorContext(crate, project, null);
        CrateDefMap defMap = buildDefMapContainingExplicitItems(context, allDependenciesDefMaps, isNormalCrate);
        if (defMap == null) return null;
        DefCollector collector = new DefCollector(project, defMap, context, pool, indicator);
        collector.collect();
        if (isNormalCrate) {
            OpenApiUtil.testAssert(() -> !FacadeMetaInfo.isCrateChanged(crate, defMap),
                () -> "DefMap " + defMap + " should be up-to-date just after built");
        }
        return defMap;
    }

    @Nullable
    private static CrateDefMap buildDefMapContainingExplicitItems(
        @NotNull CollectorContext context,
        @NotNull Map<Crate, CrateDefMap> allDependenciesDefMaps,
        boolean isNormalCrate
    ) {
        Crate crate = context.getCrate();
        Integer crateId = crate.getId();
        if (crateId == null) return null;
        RsFile crateRoot = crate.getRootMod();
        if (crateRoot == null) return null;

        if (isNormalCrate) {
            if (crateId < 0) throw new IllegalStateException("crateId must be >= 0");
            com.intellij.openapi.vfs.VirtualFile crateRootFile = crate.getRootModFile();
            if (crateRootFile == null) return null;
            if (!RsFile.shouldIndexFile(context.getProject(), crateRootFile)) return null;
        }

        RsFile.Attributes stdlibAttributes = crateRoot.getStdlibAttributes(crate);
        DependenciesDefMaps dependenciesInfo = getDependenciesDefMaps(crate, allDependenciesDefMaps, stdlibAttributes);

        String crateDescription = crate.toString();
        int rootModMacroIndex = 0;
        for (CrateDefMap depMap : allDependenciesDefMaps.values()) {
            rootModMacroIndex = Math.max(rootModMacroIndex, depMap.getRootModMacroIndex() + 1);
        }
        com.intellij.openapi.vfs.VirtualFile rootVirtualFile = isNormalCrate ? crateRoot.getVirtualFile() : null;
        Integer fileId = rootVirtualFile != null ? VirtualFileExtUtil.getFileId(rootVirtualFile) : null;
        Integer ownedDirectoryId = rootVirtualFile != null && rootVirtualFile.getParent() != null
            ? VirtualFileExtUtil.getFileId(rootVirtualFile.getParent()) : null;

        ModData crateRootData = new ModData(
            null, // parent
            crateId,
            new ModPath(crateId, new String[0]),
            new MacroIndex(new int[]{rootModMacroIndex}),
            true, // isDeeplyEnabledByCfgOuter
            RsElementUtil.isEnabledByCfgSelf(crateRoot, crate),
            fileId,
            "", // fileRelativePath
            ownedDirectoryId,
            false, // hasPathAttribute
            false, // hasMacroUse
            false, // isEnum
            isNormalCrate,
            null,  // context
            false, // isBlock
            crateDescription
        );
        CrateDefMap defMap = new CrateDefMap(
            crateId,
            crateRootData,
            dependenciesInfo.directDependencies,
            dependenciesInfo.allDependencies,
            dependenciesInfo.initialExternPrelude,
            new CrateMetaData(crate.getEdition(), crate.getNormName(), crate.getProcMacroArtifact()),
            rootModMacroIndex,
            stdlibAttributes,
            crateRoot.getRecursionLimit(crate),
            crateDescription
        );

        injectPrelude(defMap);
        Import externCrateImport = createExternCrateStdImport(defMap);
        if (externCrateImport != null) {
            context.getImports().add(externCrateImport);
            ModCollector.importExternCrateMacros(defMap, externCrateImport.getUsePath()[0]);
        }
        ModCollectorContext modCollectorContext = new ModCollectorContext(defMap, context);
        ModCollector.collectScope(crateRoot, defMap.getRoot(), modCollectorContext);

        return defMap;
    }

    @NotNull
    private static DependenciesDefMaps getDependenciesDefMaps(
        @NotNull Crate crate,
        @NotNull Map<Crate, CrateDefMap> allDependenciesDefMaps,
        @NotNull RsFile.Attributes stdlibAttributes
    ) {
        Map<Integer, CrateDefMap> allDependenciesDefMapsById = new HashMap<>();
        for (Map.Entry<Crate, CrateDefMap> entry : allDependenciesDefMaps.entrySet()) {
            Integer id = entry.getKey().getId();
            if (id != null) {
                allDependenciesDefMapsById.put(id, entry.getValue());
            }
        }

        Map<Crate.Dependency, CrateDefMap> directDependenciesByCrate = new HashMap<>();
        for (Crate.Dependency dep : crate.getDependencies()) {
            Integer id = dep.getCrate().getId();
            if (id == null) continue;
            CrateDefMap depMap = allDependenciesDefMapsById.get(id);
            if (depMap == null) continue;
            directDependenciesByCrate.put(dep, depMap);
        }

        Map<String, CrateDefMap> directDependenciesById = new HashMap<>();
        Map<String, CrateDefMap> initialExternPrelude = new HashMap<>();
        for (Map.Entry<Crate.Dependency, CrateDefMap> entry : directDependenciesByCrate.entrySet()) {
            Crate.Dependency dep = entry.getKey();
            CrateDefMap depMap = entry.getValue();
            directDependenciesById.put(dep.getNormName(), depMap);
            if (shouldAutoInjectDependency(crate, dep, stdlibAttributes)) {
                initialExternPrelude.put(dep.getNormName(), depMap);
            }
        }

        return new DependenciesDefMaps(directDependenciesById, allDependenciesDefMapsById, initialExternPrelude);
    }

    private static boolean shouldAutoInjectDependency(
        @NotNull Crate crate,
        @NotNull Crate.Dependency dependency,
        @NotNull RsFile.Attributes stdlibAttributes
    ) {
        PackageOrigin origin = crate.getOrigin();
        if (origin == PackageOrigin.STDLIB || origin == PackageOrigin.STDLIB_DEPENDENCY) return true;
        PackageOrigin depOrigin = dependency.getCrate().getOrigin();
        if (depOrigin == PackageOrigin.STDLIB) {
            String normName = dependency.getNormName();
            if ((AutoInjectedCrates.STD.equals(normName) || AutoInjectedCrates.CORE.equals(normName))
                && stdlibAttributes.canUseStdlibCrate(normName)) {
                return true;
            }
            return crate.getKind().isProcMacro() && "proc_macro".equals(normName);
        }
        if (depOrigin == PackageOrigin.STDLIB_DEPENDENCY) return false;
        return true;
    }

    private static void injectPrelude(@NotNull CrateDefMap defMap) {
        String preludeCrate = defMap.getStdlibAttributes().getAutoInjectedCrate();
        if (preludeCrate == null) return;
        String preludeName = "rust_" + defMap.getMetaData().getEdition().getPresentation();
        String[] path = new String[]{"" /* absolute path */, preludeCrate, "prelude", preludeName};
        PathResolution.ResolvePathResult result = PathResolution.resolvePathFp(defMap, defMap.getRoot(), path, ResolveMode.IMPORT, false);
        VisItem[] types = result.getResolvedDef().getTypes();
        if (types.length != 1) return;
        ModData preludeMod = defMap.tryCastToModData(types[0]);
        defMap.setPrelude(preludeMod);
    }

    @Nullable
    private static Import createExternCrateStdImport(@NotNull CrateDefMap defMap) {
        String name = defMap.getStdlibAttributes().getAutoInjectedCrate();
        if (name == null) return null;
        String nameInScope = defMap.getMetaData().getEdition() == CargoWorkspace.Edition.EDITION_2015 ? name : "_";
        return new Import(
            defMap.getRoot(),
            new String[]{name},
            nameInScope,
            defMap.getRoot().getVisibilityInSelf(),
            false, // isGlob
            true,  // isExternCrate
            false  // isPrelude
        );
    }

    /**
     * Holds the three maps needed from dependency resolution.
     */
    private static class DependenciesDefMaps {
        @NotNull final Map<String, CrateDefMap> directDependencies;
        @NotNull final Map<Integer, CrateDefMap> allDependencies;
        @NotNull final Map<String, CrateDefMap> initialExternPrelude;

        DependenciesDefMaps(
            @NotNull Map<String, CrateDefMap> directDependencies,
            @NotNull Map<Integer, CrateDefMap> allDependencies,
            @NotNull Map<String, CrateDefMap> initialExternPrelude
        ) {
            this.directDependencies = directDependencies;
            this.allDependencies = allDependencies;
            this.initialExternPrelude = initialExternPrelude;
        }
    }
}
