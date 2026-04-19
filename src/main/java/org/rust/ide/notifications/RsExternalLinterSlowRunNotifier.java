/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.notifications;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.registry.RegistryValue;
import com.intellij.openapi.util.text.HtmlChunk;
import com.intellij.openapi.wm.WindowManager;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.cargo.project.settings.ExternalLinterSettingsUtil;
import org.rust.ide.status.RsExternalLinterWidget;

import javax.swing.event.HyperlinkEvent;
import java.util.ArrayDeque;
import java.util.Queue;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;

@Service
public final class RsExternalLinterSlowRunNotifier {

    private static final int MAX_QUEUE_SIZE = 5;
    private static final String DO_NOT_SHOW_KEY = "org.rust.external.linter.slow.run.do.not.show";
    private static final RegistryValue LINTER_MAX_DURATION = Registry.get("org.rust.external.linter.max.duration");

    private final Project myProject;
    private final Queue<Long> myPrevDurations = new ArrayDeque<>();

    public RsExternalLinterSlowRunNotifier(@NotNull Project project) {
        myProject = project;
    }

    public void reportDuration(long duration) {
        myPrevDurations.add(duration);
        while (myPrevDurations.size() > MAX_QUEUE_SIZE) {
            myPrevDurations.remove();
        }

        if (PropertiesComponent.getInstance().getBoolean(DO_NOT_SHOW_KEY, false)) return;

        long minPrevDuration = Long.MAX_VALUE;
        for (Long d : myPrevDurations) {
            if (d < minPrevDuration) minPrevDuration = d;
        }
        if (minPrevDuration == Long.MAX_VALUE) minPrevDuration = 0;

        int maxDuration = LINTER_MAX_DURATION.asInteger();
        if (myPrevDurations.size() == MAX_QUEUE_SIZE && minPrevDuration > maxDuration) {
            com.intellij.openapi.wm.StatusBar statusBar = WindowManager.getInstance().getStatusBar(myProject);
            if (statusBar == null) return;
            com.intellij.openapi.wm.StatusBarWidget widget = statusBar.getWidget(RsExternalLinterWidget.ID);
            if (!(widget instanceof RsExternalLinterWidget)) return;
            RsExternalLinterWidget linterWidget = (RsExternalLinterWidget) widget;
            String content = RsBundle.message("notification.content.low.performance.due.to.rust.external.linter.nbsp.nbsp.nbsp.nbsp",
                HtmlChunk.br(),
                HtmlChunk.link("disable", RsBundle.message("disable")),
                HtmlChunk.link("dont-show-again", RsBundle.message("don.t.show.again")));
            NotificationUtils.showBalloon(myProject, content, com.intellij.notification.NotificationType.WARNING, new com.intellij.openapi.actionSystem.AnAction(RsBundle.message("disable")) {
                @Override
                public void actionPerformed(@org.jetbrains.annotations.NotNull com.intellij.openapi.actionSystem.AnActionEvent e) {
                    RsProjectSettingsServiceUtil.getExternalLinterSettings(myProject).modify(it -> {
                        it.runOnTheFly = false;
                    });
                }
            });
        }
    }
}
