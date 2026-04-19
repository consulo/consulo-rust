/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.crate.Crate;

/**
 * happens in {@link FacadeUpdateDefMap}.
 */
public final class FacadeUpdateDefMapUtil {
    private FacadeUpdateDefMapUtil() {}

    public static void forceRebuildDefMapForAllCrates(@NotNull Project project, boolean multithread) {
        FacadeUpdateDefMap.forceRebuildDefMapForAllCrates(project, multithread);
    }

    public static void forceRebuildDefMapForCrate(@NotNull Project project, @NotNull Object crateId) {
        if (crateId instanceof Integer) {
            FacadeUpdateDefMap.forceRebuildDefMapForCrate(project, (Integer) crateId);
        }
    }

    @Nullable
    public static CrateDefMap getOrUpdateIfNeeded(@NotNull Object crate) {
        if (!(crate instanceof Crate)) return null;
        Crate c = (Crate) crate;
        Integer id = c.getId();
        if (id == null) return null;
        DefMapService service = c.getProject().getService(DefMapService.class);
        return FacadeUpdateDefMap.getOrUpdateIfNeeded(service, id);
    }

    @Nullable
    public static CrateDefMap getOrUpdateIfNeeded(@NotNull Object service, int crateId) {
        if (!(service instanceof DefMapService)) return null;
        return FacadeUpdateDefMap.getOrUpdateIfNeeded((DefMapService) service, crateId);
    }

    @Nullable
    public static CrateDefMap getOrUpdateIfNeeded(@NotNull Object service, @NotNull Object crateId) {
        if (!(service instanceof DefMapService)) return null;
        if (crateId instanceof Integer) {
            return FacadeUpdateDefMap.getOrUpdateIfNeeded((DefMapService) service, (Integer) crateId);
        }
        return null;
    }
}
