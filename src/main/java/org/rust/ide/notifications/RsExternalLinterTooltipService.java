/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.notifications;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.ui.GotItTooltip;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.cargo.project.configurable.RsExternalLinterConfigurable;
import org.rust.cargo.project.settings.ExternalLinterSettingsUtil;
import org.rust.cargo.toolchain.ExternalLinter;
import org.rust.openapiext.OpenApiUtil;

import javax.swing.JComponent;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;

@Service
public final class RsExternalLinterTooltipService implements Disposable {

    private final Project myProject;

    public RsExternalLinterTooltipService(@NotNull Project project) {
        myProject = project;
    }

    public void showTooltip(@NotNull JComponent component) {
        boolean turnedOn = RsProjectSettingsServiceUtil.getExternalLinterSettings(myProject).getRunOnTheFly();
        GotItTooltip tooltip = createTooltip(turnedOn);
        tooltip.show(component, GotItTooltip.TOP_MIDDLE);
    }

    @Override
    public void dispose() {
    }

    @NotNull
    private GotItTooltip createTooltip(boolean turnedOn) {
        ExternalLinter linter = RsProjectSettingsServiceUtil.getExternalLinterSettings(myProject).getTool();
        String headerText = RsBundle.message("0.on.the.fly.analysis.is.turned.1.choice.0.on.1.off", linter.getTitle(), turnedOn ? 0 : 1);
        String text = RsBundle.message("external.linter.tooltip", linter.getTitle());
        return new GotItTooltip("rust.linter.on-the-fly.got.it", text, this)
            .withHeader(headerText)
            .withLink(RsBundle.message("configure"), () -> {
                OpenApiUtil.showSettingsDialog(myProject, RsExternalLinterConfigurable.class);
            });
    }
}
