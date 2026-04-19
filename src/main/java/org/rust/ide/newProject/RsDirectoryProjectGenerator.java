/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.newProject;

import com.intellij.facet.ui.ValidationResult;
import com.intellij.ide.util.projectWizard.AbstractNewProjectStep;
import com.intellij.ide.util.projectWizard.CustomStepProjectGenerator;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.impl.welcomeScreen.AbstractActionWithPanel;
import com.intellij.platform.DirectoryProjectGenerator;
import com.intellij.platform.DirectoryProjectGeneratorBase;
import com.intellij.platform.ProjectGeneratorPeer;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.toolchain.tools.Cargo;
import org.rust.ide.icons.RsIcons;
import org.rust.openapiext.OpenApiUtil;
import org.rust.stdext.RsResult;

import javax.swing.*;

// We implement `CustomStepProjectGenerator` as well to correctly show settings UI
// because otherwise PyCharm doesn't add peer's component into project settings panel
public class RsDirectoryProjectGenerator extends DirectoryProjectGeneratorBase<ConfigurationData>
    implements CustomStepProjectGenerator<ConfigurationData> {

    @Nullable
    private RsProjectGeneratorPeer peer;

    @NotNull
    @Override
    public String getName() {
        return RsBundle.message("rust");
    }

    @NotNull
    @Override
    public Icon getLogo() {
        return RsIcons.RUST;
    }

    @NotNull
    @Override
    public ProjectGeneratorPeer<ConfigurationData> createPeer() {
        RsProjectGeneratorPeer newPeer = new RsProjectGeneratorPeer();
        this.peer = newPeer;
        return newPeer;
    }

    @NotNull
    @Override
    public ValidationResult validate(@NotNull String baseDirPath) {
        String crateName = PathUtil.getFileName(baseDirPath);
        if (peer == null) return ValidationResult.OK;
        ConfigurationData settings = peer.getSettings();
        if (settings == null) return ValidationResult.OK;
        String message = settings.getTemplate().validateProjectName(crateName);
        if (message == null) return ValidationResult.OK;
        return new ValidationResult(message);
    }

    @Override
    public void generateProject(
        @NotNull Project project,
        @NotNull VirtualFile baseDir,
        @NotNull ConfigurationData data,
        @NotNull Module module
    ) {
        var settings = data.getSettings();
        var toolchain = settings.getToolchain();
        if (toolchain == null) return;
        Cargo cargo = Cargo.cargo(toolchain);

        String name = project.getName().replace(' ', '_');
        var template = data.getTemplate();
        Cargo.GeneratedFilesHolder generatedFiles = OpenApiUtil.computeWithCancelableProgress(
            project,
            RsBundle.message("progress.title.generating.cargo.project"),
            () -> RsResult.unwrapOrThrow(Utils.makeProject(cargo, project, module, baseDir, name, template))
        );

        RsProjectSettingsServiceUtil.getRustSettings(project).modify(state -> {
            state.setToolchain(settings.getToolchain());
            state.explicitPathToStdlib = settings.getExplicitPathToStdlib();
        });

        Utils.makeDefaultRunConfiguration(project, template);
        Utils.openFiles(project, generatedFiles);
    }

    @NotNull
    @Override
    public AbstractActionWithPanel createStep(
        @NotNull DirectoryProjectGenerator<ConfigurationData> projectGenerator,
        @NotNull AbstractNewProjectStep.AbstractCallback<ConfigurationData> callback
    ) {
        return new RsProjectSettingsStep(projectGenerator);
    }
}
