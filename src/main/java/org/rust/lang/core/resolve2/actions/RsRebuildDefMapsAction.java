/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2.actions;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.ide.notifications.NotificationUtils;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.crate.impl.FakeCrate;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.resolve2.FacadeUpdateDefMapUtil;
import org.rust.openapiext.OpenApiUtil;

/**
 * Contains both RsRebuildAllDefMapsAction and RsRebuildCurrentDefMapAction
 * <p>
 * Since Java does not support multiple top-level classes in the same file,
 * the actual action classes are defined in separate files:
 * {@link RsRebuildAllDefMapsAction} and {@link RsRebuildCurrentDefMapAction}.
 * <p>
 */
public final class RsRebuildDefMapsAction {

    private RsRebuildDefMapsAction() {}

    /**
     * Performs the "rebuild all def maps" action.
     * Delegates to {@link RsRebuildAllDefMapsAction}.
     */
    public static void rebuildAllDefMaps(@NotNull Project project) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            long start = System.currentTimeMillis();
            FacadeUpdateDefMapUtil.forceRebuildDefMapForAllCrates(project, false);
            long time = System.currentTimeMillis() - start;
            NotificationUtils.showBalloon(
                project,
                RsBundle.message("notification.content.rebuilt.defmap.for.all.crates.in.ms", time),
                NotificationType.INFORMATION
            );
        });
    }

    /**
     * Performs the "rebuild current def map" action.
     * Delegates to {@link RsRebuildCurrentDefMapAction}.
     */
    public static void rebuildCurrentDefMap(@NotNull Project project, @NotNull RsFile file) {
        Crate crate = file.getCrate();
        if (crate instanceof FakeCrate) return;
        Integer crateId = crate.getId();
        if (crateId == null) return;
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            long start = System.currentTimeMillis();
            FacadeUpdateDefMapUtil.forceRebuildDefMapForCrate(project, crateId);
            long time = System.currentTimeMillis() - start;
            NotificationUtils.setStatusBarText(
                project,
                RsBundle.message("status.bar.text.rebuilt.defmap.for.in.ms", crate, time)
            );
        });
    }
}
