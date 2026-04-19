/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.status;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.CustomStatusBarWidget;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.impl.status.TextPanel;
import com.intellij.ui.ClickListener;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.cargo.project.configurable.RsExternalLinterConfigurable;
import org.rust.cargo.project.settings.RsExternalLinterProjectSettingsService;
import org.rust.cargo.project.settings.RsProjectSettingsServiceBase;
import org.rust.cargo.project.settings.RsProjectSettingsServiceBase.RsSettingsListener;
import org.rust.cargo.project.settings.RsProjectSettingsServiceBase.SettingsChangedEventBase;
import org.rust.cargo.toolchain.ExternalLinter;
import org.rust.ide.icons.RsIcons;
import org.rust.ide.notifications.RsExternalLinterTooltipService;
import org.rust.openapiext.OpenApiUtil;

import javax.swing.*;
import java.awt.event.MouseEvent;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;

public class RsExternalLinterWidget extends TextPanel.WithIconAndArrows implements CustomStatusBarWidget {

    public static final String ID = "rustExternalLinterWidget";

    @NotNull
    private final Project project;
    private StatusBar statusBar;
    private boolean inProgress;

    public RsExternalLinterWidget(@NotNull Project project) {
        this.project = project;
        setTextAlignment(CENTER_ALIGNMENT);
        setBorder(JBUI.CurrentTheme.StatusBar.Widget.border());
    }

    @NotNull
    private ExternalLinter getLinter() {
        return RsProjectSettingsServiceUtil.getExternalLinterSettings(project).getTool();
    }

    private boolean isTurnedOn() {
        return RsProjectSettingsServiceUtil.getExternalLinterSettings(project).getRunOnTheFly();
    }

    public boolean isInProgress() {
        return inProgress;
    }

    public void setInProgress(boolean value) {
        this.inProgress = value;
        update();
    }

    @Override
    @NotNull
    public String ID() {
        return ID;
    }

    @Override
    public void install(@NotNull StatusBar statusBar) {
        this.statusBar = statusBar;

        if (!project.isDisposed()) {
            new ClickListener() {
                @Override
                public boolean onClick(@NotNull MouseEvent event, int clickCount) {
                    if (!project.isDisposed()) {
                        OpenApiUtil.showSettingsDialog(project, RsExternalLinterConfigurable.class);
                    }
                    return true;
                }
            }.installOn(this, true);

            project.getMessageBus().connect(this).subscribe(
                RsProjectSettingsServiceBase.RUST_SETTINGS_TOPIC,
                new RsSettingsListener() {
                    @Override
                    public <T extends RsProjectSettingsServiceBase.RsProjectSettingsBase<T>> void settingsChanged(
                        @NotNull SettingsChangedEventBase<T> e
                    ) {
                        if (!(e instanceof RsExternalLinterProjectSettingsService.SettingsChangedEvent)) return;
                        RsExternalLinterProjectSettingsService.SettingsChangedEvent event =
                            (RsExternalLinterProjectSettingsService.SettingsChangedEvent) e;
                        RsExternalLinterProjectSettingsService.RsExternalLinterProjectSettings oldState = event.getOldState();
                        RsExternalLinterProjectSettingsService.RsExternalLinterProjectSettings newState = event.getNewState();
                        if (oldState.tool != newState.tool || oldState.runOnTheFly != newState.runOnTheFly) {
                            update();
                        }
                    }
                }
            );

            project.getService(RsExternalLinterTooltipService.class).showTooltip(this);
        }

        update();
        statusBar.updateWidget(ID());
    }

    @Override
    public void dispose() {
        statusBar = null;
        UIUtil.dispose(this);
    }

    @Override
    @NotNull
    public JComponent getComponent() {
        return this;
    }

    private void update() {
        if (project.isDisposed()) return;
        UIUtil.invokeLaterIfNeeded(() -> {
            if (project.isDisposed()) return;
            ExternalLinter linter = getLinter();
            boolean turnedOn = isTurnedOn();
            setText(linter.getTitle());
            String status = turnedOn ? RsBundle.message("on") : RsBundle.message("off");
            setToolTipText(RsBundle.message("0.2.choice.0.is.in.progress.1.on.the.fly.analysis.is.turned.1",
                linter.getTitle(), status, inProgress ? 0 : 1));
            if (!turnedOn) {
                setIcon(RsIcons.GEAR_OFF);
            } else if (inProgress) {
                setIcon(RsIcons.GEAR_ANIMATED);
            } else {
                setIcon(RsIcons.GEAR);
            }
            repaint();
        });
    }

    /** @deprecated Use {@link RsExternalLinterWidgetFactory} directly. */
    @Deprecated
    public static class Factory extends RsExternalLinterWidgetFactory {
    }

    /** @deprecated Use {@link RsExternalLinterWidgetUpdater} directly. */
    @Deprecated
    public static class Updater extends RsExternalLinterWidgetUpdater {
        public Updater(@NotNull Project project) {
            super(project);
        }
    }
}
