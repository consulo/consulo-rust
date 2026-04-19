/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.ext.RsElement;

public final class RsPsiManagerUtil {
    private RsPsiManagerUtil() {
    }

    private static final Topic<RustStructureChangeListener> RUST_STRUCTURE_CHANGE_TOPIC =
        Topic.create("RUST_STRUCTURE_CHANGE_TOPIC", RustStructureChangeListener.class);

    private static final Topic<RustPsiChangeListener> RUST_PSI_CHANGE_TOPIC =
        Topic.create("RUST_PSI_CHANGE_TOPIC", RustPsiChangeListener.class);

    @NotNull
    public static Topic<RustStructureChangeListener> getRUST_STRUCTURE_CHANGE_TOPIC() {
        return RUST_STRUCTURE_CHANGE_TOPIC;
    }

    @NotNull
    public static Topic<RustPsiChangeListener> getRUST_PSI_CHANGE_TOPIC() {
        return RUST_PSI_CHANGE_TOPIC;
    }

    @NotNull
    public static RsPsiManager getRustPsiManager(@NotNull Project project) {
        return project.getService(RsPsiManager.class);
    }

    @NotNull
    public static ModificationTracker getRustStructureModificationTracker(@NotNull Project project) {
        return getRustPsiManager(project).getRustStructureModificationTracker();
    }

    @NotNull
    public static ModificationTracker getRustStructureModificationTracker(@NotNull Crate crate) {
        return getRustStructureModificationTracker(crate.getProject());
    }

    @NotNull
    public static ModificationTracker getRustStructureOrAnyPsiModificationTracker(@NotNull RsElement element) {
        Project project = element.getProject();
        return getRustPsiManager(project).getRustStructureModificationTracker();
    }
}
