/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.notifications;

import consulo.project.Project;
import consulo.application.util.registry.Registry;
import consulo.application.util.registry.RegistryValue;
import consulo.application.util.HtmlChunk;
import consulo.project.ui.wm.WindowManager;
import consulo.application.ApplicationManager;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.ide.status.RsExternalLinterWidget;

import javax.swing.event.HyperlinkEvent;
import java.util.ArrayDeque;
import java.util.Queue;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.annotation.component.ComponentScope;
import jakarta.inject.Inject;
import consulo.application.ApplicationPropertiesComponent;
import consulo.project.ui.notification.NotificationType;
import consulo.project.ui.wm.StatusBar;

@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public final class RsExternalLinterSlowRunNotifier {

    private static final int MAX_QUEUE_SIZE = 5;
    private static final String DO_NOT_SHOW_KEY = "org.rust.external.linter.slow.run.do.not.show";
    private static final String DISABLE_LINK = "disable";
    private static final String DO_NOT_SHOW_LINK = "dont-show-again";
    /** Warn when the external linter runs longer than this, unless the registry key overrides it. */
    private static final int DEFAULT_MAX_DURATION_MS = 3000;

    private static final RegistryValue LINTER_MAX_DURATION = Registry.get("org.rust.external.linter.max.duration");

    private final Project myProject;
    private final Queue<Long> myPrevDurations = new ArrayDeque<>();

    @Inject

    public RsExternalLinterSlowRunNotifier(@Nonnull Project project) {
        myProject = project;
    }

    public void reportDuration(long duration) {
        myPrevDurations.add(duration);
        while (myPrevDurations.size() > MAX_QUEUE_SIZE) {
            myPrevDurations.remove();
        }

        if (ApplicationPropertiesComponent.getInstance().getBoolean(DO_NOT_SHOW_KEY, false)) return;

        long minPrevDuration = Long.MAX_VALUE;
        for (Long d : myPrevDurations) {
            if (d < minPrevDuration) minPrevDuration = d;
        }
        if (minPrevDuration == Long.MAX_VALUE) minPrevDuration = 0;

        int maxDuration = LINTER_MAX_DURATION.asInteger(DEFAULT_MAX_DURATION_MS);
        if (myPrevDurations.size() == MAX_QUEUE_SIZE && minPrevDuration > maxDuration) {
            consulo.project.ui.wm.StatusBar statusBar = WindowManager.getInstance().getStatusBar(myProject);
            if (statusBar == null) return;
            RsExternalLinterWidget linterWidget = statusBar.<RsExternalLinterWidget>findWidget(w -> w instanceof RsExternalLinterWidget).orElse(null);
            if (linterWidget == null) return;
            String content = RsBundle.message("notification.content.low.performance.due.to.rust.external.linter.nbsp.nbsp.nbsp.nbsp",
                HtmlChunk.br(),
                HtmlChunk.link(DISABLE_LINK, RsBundle.message("disable")),
                HtmlChunk.link(DO_NOT_SHOW_LINK, RsBundle.message("don.t.show.again")));
            ApplicationManager.getApplication().invokeLater(() -> {
                if (myProject.isDisposed()) return;
                NotificationUtils.showComponentBalloon(
                    linterWidget,
                    content,
                    NotificationType.WARNING,
                    myProject,
                    this::handleLink
                );
            });
        }
    }

    private void handleLink(@Nonnull HyperlinkEvent event) {
        if (event.getEventType() != HyperlinkEvent.EventType.ACTIVATED) return;
        String description = event.getDescription();
        if (DISABLE_LINK.equals(description)) {
            RsProjectSettingsServiceUtil.getExternalLinterSettings(myProject).modify(it -> {
                it.runOnTheFly = false;
            });
        }
        else if (DO_NOT_SHOW_LINK.equals(description)) {
            ApplicationPropertiesComponent.getInstance().setValue(DO_NOT_SHOW_KEY, true, false);
        }
    }
}
