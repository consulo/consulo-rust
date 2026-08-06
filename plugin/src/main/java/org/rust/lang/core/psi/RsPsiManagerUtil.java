/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import consulo.project.Project;
import consulo.component.util.ModificationTracker;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.ext.RsElement;

public final class RsPsiManagerUtil {
    private RsPsiManagerUtil() {
    }

    private static final Class<RustStructureChangeListener> RUST_STRUCTURE_CHANGE_TOPIC =
        RustStructureChangeListener.class;

    private static final Class<RustPsiChangeListener> RUST_PSI_CHANGE_TOPIC =
        RustPsiChangeListener.class;

    @Nonnull
    public static Class<RustStructureChangeListener> getRUST_STRUCTURE_CHANGE_TOPIC() {
        return RUST_STRUCTURE_CHANGE_TOPIC;
    }

    @Nonnull
    public static Class<RustPsiChangeListener> getRUST_PSI_CHANGE_TOPIC() {
        return RUST_PSI_CHANGE_TOPIC;
    }

    @Nonnull
    public static RsPsiManager getRustPsiManager(@Nonnull Project project) {
        return project.getService(RsPsiManager.class);
    }

    @Nonnull
    public static ModificationTracker getRustStructureModificationTracker(@Nonnull Project project) {
        return getRustPsiManager(project).getRustStructureModificationTracker();
    }

    @Nonnull
    public static ModificationTracker getRustStructureModificationTracker(@Nonnull Crate crate) {
        return getRustStructureModificationTracker(crate.getProject());
    }

    @Nonnull
    public static ModificationTracker getRustStructureOrAnyPsiModificationTracker(@Nonnull RsElement element) {
        Project project = element.getProject();
        return getRustPsiManager(project).getRustStructureModificationTracker();
    }
}
