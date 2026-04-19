/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.crate.CratePersistentId;
import org.rust.lang.core.crate.impl.CargoBasedCrate;
import org.rust.lang.core.crate.impl.DoctestCrate;
import org.rust.lang.core.crate.impl.FakeDetachedCrate;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.RsModItem;
import org.rust.lang.core.psi.ext.RsItemsOwner;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.RsModUtil;
import org.rust.openapiext.OpenApiUtil;

import java.lang.ref.SoftReference;
import java.util.*;

/**
 * Provides functions for resolving module info in detached and doctest crates.
 */
public final class DetachedDefMap {

    private DetachedDefMap() {}

    private static final Key<SoftReference<Object[]>> DEF_MAP_KEY = Key.create("DEF_MAP_KEY");

    @Nullable
    public static RsModInfo getDetachedModInfo(@NotNull Project project, @NotNull RsMod scope, @NotNull FakeDetachedCrate crate) {
        return getModInfoInDetachedCrate(project, scope, crate);
    }

    @Nullable
    public static RsModInfo getDoctestModInfo(@NotNull Project project, @NotNull RsMod scope, @NotNull DoctestCrate crate) {
        return getModInfoInDetachedCrate(project, scope, crate);
    }

    @Nullable
    private static RsModInfo getModInfoInDetachedCrate(@NotNull Project project, @NotNull RsMod scope, @NotNull Crate crate) {
        DefMapService defMapService = project.getService(DefMapService.class);
        CrateDefMap defMap = cachedGetDefMapForNonCargoCrate(defMapService, crate);
        if (defMap == null) return null;
        RsFile rootMod = crate.getRootMod();
        if (rootMod == null) return null;
        DetachedFileDataPsiHelper dataPsiHelper = new DetachedFileDataPsiHelper(rootMod, defMap);
        ModData modData = dataPsiHelper.psiToData(scope);
        if (modData == null) return null;
        return new RsModInfo(project, defMap, modData, crate, dataPsiHelper);
    }

    @Nullable
    private static CrateDefMap cachedGetDefMapForNonCargoCrate(@NotNull DefMapService defMapService, @NotNull Crate crate) {
        if (crate.getId() == null) throw new IllegalStateException("Crate id must not be null");
        if (crate instanceof CargoBasedCrate) throw new IllegalStateException("Crate must not be CargoBasedCrate");
        RsFile crateRoot = crate.getRootMod();
        if (crateRoot == null) return null;

        Map<Crate, CrateDefMap> allDependenciesDefMaps = getAllDependenciesDefMaps(crate);

        com.intellij.openapi.progress.ProgressIndicator indicator =
            ProgressManager.getGlobalProgressIndicator() != null
                ? ProgressManager.getGlobalProgressIndicator()
                : new EmptyProgressIndicator();
        return FacadeBuildDefMap.buildDefMap(crate, allDependenciesDefMaps, null, indicator, false);
    }

    @NotNull
    private static Map<Crate, CrateDefMap> getAllDependenciesDefMaps(@NotNull Crate crate) {
        Collection<Crate> allDependencies = crate.getFlatDependencies();
        List<Integer> ids = new ArrayList<>();
        Map<Integer, Crate> crateById = new HashMap<>();
        for (Crate dep : allDependencies) {
            Integer id = dep.getId();
            if (id != null) {
                ids.add(id);
                crateById.put(id, dep);
            }
        }
        DefMapService defMapService = crate.getProject().getService(DefMapService.class);
        Map<Integer, CrateDefMap> defMapById = FacadeUpdateDefMap.getOrUpdateIfNeeded(defMapService, ids);
        Map<Crate, CrateDefMap> result = new HashMap<>();
        for (Map.Entry<Integer, CrateDefMap> entry : defMapById.entrySet()) {
            Crate c = crateById.get(entry.getKey());
            CrateDefMap dm = entry.getValue();
            if (c != null && dm != null) {
                result.put(c, dm);
            }
        }
        return result;
    }

    /**
     * DataPsiHelper implementation for detached files.
     */
    private static class DetachedFileDataPsiHelper implements DataPsiHelper {
        @NotNull
        private final RsFile root;
        @NotNull
        private final CrateDefMap defMap;

        DetachedFileDataPsiHelper(@NotNull RsFile root, @NotNull CrateDefMap defMap) {
            this.root = root;
            this.defMap = defMap;
        }

        @Override
        @Nullable
        public ModData psiToData(@NotNull RsItemsOwner scope) {
            if (scope.getContainingFile() != root) return null;
            if (scope == root) return defMap.getRoot();
            if (scope instanceof RsModItem) {
                RsMod superMod = ((RsModItem) scope).getSuper();
                if (superMod == null) return null;
                ModData superModData = psiToData(superMod);
                if (superModData == null) return null;
                return superModData.getChildModules().get(RsModUtil.getModName((RsModItem) scope));
            }
            return null;
        }

        @Override
        @Nullable
        public RsMod dataToPsi(@NotNull ModData data) {
            if (data.getCrate() != defMap.getCrate()) return null;
            if (data == defMap.getRoot()) return root;
            ModData superModData = data.getParent();
            if (superModData == null) return null;
            RsMod superMod = dataToPsi(superModData);
            if (superMod == null) return null;
            return RsModUtil.getChildModule(superMod, data.getName());
        }
    }
}
