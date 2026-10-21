/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.status;

import consulo.ide.setting.ShowSettingsUtil;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.ui.Component;
import consulo.ui.event.ClickEvent;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.ex.awt.UIUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.api.settings.*;
import org.rust.cargo.api.settings.RsProjectSettingsServiceBase.SettingsChangedEventBase;
import org.rust.cargo.api.toolchain.ExternalLinter;
import org.rust.cargo.project.configurable.RsExternalLinterConfigurable;
import org.rust.ide.notifications.RsExternalLinterTooltipService;

public class RsExternalLinterWidget implements StatusBarWidget, StatusBarWidget.TextPresentation {

    public static final String ID = "rustExternalLinterWidget";

    @Nonnull
    private final Project project;
    private boolean inProgress;

    private String myText;
    private LocalizeValue myToolTipText = LocalizeValue.empty();

    @Nonnull
    @Override
    public String getId() {
        return ID;
    }

    @Nullable
    @Override
    public StatusBarWidget.WidgetPresentation getPresentation() {
        return this;
    }

    public RsExternalLinterWidget(@Nonnull Project project) {
        this.project = project;
    }

    @Nonnull
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
    public @Nullable ComponentEventListener<Component, ClickEvent> getClickListener() {
        return clickEvent -> {
            if (!project.isDisposed()) {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, RsExternalLinterConfigurable.class);
            }
        };
    }

    @Override
    public void install(@Nonnull StatusBar statusBar) {
        project.getMessageBus().connect(this).subscribe(
            RsProjectSettingsServiceBase.RUST_SETTINGS_TOPIC,
            new RsSettingsListener() {
                @Override
                public void settingsChanged(
                    @Nonnull SettingsChangedEventBase<?> e
                ) {
                    if (!(e instanceof RsExternalLinterProjectSettingsService.SettingsChangedEvent)) {
                        return;
                    }
                    RsExternalLinterProjectSettingsService.SettingsChangedEvent event =
                        (RsExternalLinterProjectSettingsService.SettingsChangedEvent) e;
                    RsExternalLinterProjectSettings oldState = event.getOldState();
                    RsExternalLinterProjectSettings newState = event.getNewState();
                    if (oldState.tool != newState.tool || oldState.runOnTheFly != newState.runOnTheFly) {
                        update();
                    }
                }
            }
        );

        update();
        statusBar.updateWidget(getId());

        project.getUIAccess().give(() -> {
            if (!project.isDisposed()) {
                project.getInstance(RsExternalLinterTooltipService.class).showTooltip(statusBar, getId());
            }
        });
    }

    @Override
    public void dispose() {
    }

    @Override
    public String getText() {
        return myText;
    }

    public LocalizeValue getTooltipText() {
        return myToolTipText;
    }

    private void update() {
        if (project.isDisposed()) {
            return;
        }

        ExternalLinter linter = getLinter();
        boolean turnedOn = isTurnedOn();
        myText = linter.getTitle();
        String status = turnedOn ? RsBundle.message("on") : RsBundle.message("off");
        myToolTipText = LocalizeValue.localizeTODO(RsBundle.message("0.2.choice.0.is.in.progress.1.on.the.fly.analysis.is.turned.1", linter.getTitle(), status, inProgress ? 0 : 1));
    }
}
