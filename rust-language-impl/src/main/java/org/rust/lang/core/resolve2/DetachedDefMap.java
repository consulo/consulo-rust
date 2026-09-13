/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.crate.impl.CargoBasedCrate;
import org.rust.lang.core.crate.impl.DoctestCrate;
import org.rust.lang.core.crate.impl.FakeDetachedCrate;
import org.rust.lang.core.psi.impl.RsFile;
import org.rust.lang.core.psi.RsModItem;
import org.rust.lang.core.psi.ext.RsItemsOwner;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.impl.RsModUtil;

import java.lang.ref.SoftReference;
import java.util.*;
import consulo.application.progress.ProgressIndicator;

/**
 * Provides functions for resolving module info in detached and doctest crates.
 */
public final class DetachedDefMap {

    private DetachedDefMap() {}

    private static final Key<SoftReference<Object[]>> DEF_MAP_KEY = Key.create("DEF_MAP_KEY");

    @Nullable
    public static RsModInfo getDetachedModInfo(@Nonnull Project project, @Nonnull RsMod scope, @Nonnull FakeDetachedCrate crate) {
        return getModInfoInDetachedCrate(project, scope, crate);
    }

    @Nullable
    public static RsModInfo getDoctestModInfo(@Nonnull Project project, @Nonnull RsMod scope, @Nonnull DoctestCrate crate) {
        return getModInfoInDetachedCrate(project, scope, crate);
    }

    @Nullable
    private static RsModInfo getModInfoInDetachedCrate(@Nonnull Project project, @Nonnull RsMod scope, @Nonnull Crate crate) {
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
    private static CrateDefMap cachedGetDefMapForNonCargoCrate(@Nonnull DefMapService defMapService, @Nonnull Crate crate) {
        if (crate.getId() == null) throw new IllegalStateException("Crate id must not be null");
        if (crate instanceof CargoBasedCrate) throw new IllegalStateException("Crate must not be CargoBasedCrate");
        RsFile crateRoot = crate.getRootMod();
        if (crateRoot == null) return null;

        Map<Crate, CrateDefMap> allDependenciesDefMaps = getAllDependenciesDefMaps(crate);

        consulo.application.progress.ProgressIndicator indicator =
            ProgressManager.getGlobalProgressIndicator() != null
                ? ProgressManager.getGlobalProgressIndicator()
                : new EmptyProgressIndicator();
        return FacadeBuildDefMap.buildDefMap(crate, allDependenciesDefMaps, null, indicator, false);
    }

    @Nonnull
    private static Map<Crate, CrateDefMap> getAllDependenciesDefMaps(@Nonnull Crate crate) {
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
        @Nonnull
        private final RsFile root;
        @Nonnull
        private final CrateDefMap defMap;

        DetachedFileDataPsiHelper(@Nonnull RsFile root, @Nonnull CrateDefMap defMap) {
            this.root = root;
            this.defMap = defMap;
        }

        @Override
        @Nullable
        public ModData psiToData(@Nonnull RsItemsOwner scope) {
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
        public RsMod dataToPsi(@Nonnull ModData data) {
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
