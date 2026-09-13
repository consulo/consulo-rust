/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.notifications;

import consulo.ide.setting.ShowSettingsUtil;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationPropertiesComponent;
import consulo.application.util.HtmlChunk;
import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationType;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.rust.RsBundle;
import org.rust.cargo.project.configurable.RsExternalLinterConfigurable;
import org.rust.cargo.api.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.api.toolchain.ExternalLinter;
import org.rust.openapiext.OpenApiUtil;

import javax.swing.JComponent;
import javax.swing.event.HyperlinkEvent;
import org.rust.notifications.NotificationUtils;

@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public final class RsExternalLinterTooltipService implements Disposable {

    /** Remembers that the tooltip has already been shown, so it is only presented once per installation. */
    private static final String ALREADY_SHOWN_KEY = "rust.linter.on-the-fly.got.it";
    private static final String CONFIGURE_LINK = "configure";

    private final Project myProject;

    @Inject
    public RsExternalLinterTooltipService(@Nonnull Project project) {
        myProject = project;
    }

    public void showTooltip(@Nonnull JComponent component) {
        ApplicationPropertiesComponent properties = ApplicationPropertiesComponent.getInstance();
        if (properties.getBoolean(ALREADY_SHOWN_KEY, false)) return;
        properties.setValue(ALREADY_SHOWN_KEY, true, false);

        NotificationUtils.showComponentBalloon(
            component,
            buildContent(),
            NotificationType.INFORMATION,
            myProject,
            this::handleLink
        );
    }

    @Override
    public void dispose() {
    }

    @Nonnull
    private String buildContent() {
        ExternalLinter linter = RsProjectSettingsServiceUtil.getExternalLinterSettings(myProject).getTool();
        boolean turnedOn = RsProjectSettingsServiceUtil.getExternalLinterSettings(myProject).getRunOnTheFly();
        String headerText = RsBundle.message("0.on.the.fly.analysis.is.turned.1.choice.0.on.1.off", linter.getTitle(), turnedOn ? 0 : 1);
        String text = RsBundle.message("external.linter.tooltip", linter.getTitle());

        StringBuilder builder = new StringBuilder();
        HtmlChunk.text(headerText).bold().appendTo(builder);
        HtmlChunk.br().appendTo(builder);
        HtmlChunk.text(text).appendTo(builder);
        HtmlChunk.br().appendTo(builder);
        HtmlChunk.link(CONFIGURE_LINK, RsBundle.message("configure")).appendTo(builder);
        return builder.toString();
    }

    private void handleLink(@Nonnull HyperlinkEvent event) {
        if (event.getEventType() != HyperlinkEvent.EventType.ACTIVATED) return;
        if (CONFIGURE_LINK.equals(event.getDescription())) {
            ShowSettingsUtil.getInstance().showSettingsDialog(myProject, RsExternalLinterConfigurable.class);
        }
    }
}
