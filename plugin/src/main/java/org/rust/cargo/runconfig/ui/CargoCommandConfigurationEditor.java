/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.ui;

import consulo.configurable.ConfigurationException;
import consulo.execution.localize.ExecutionLocalize;
import consulo.execution.ui.awt.EnvironmentVariablesTextFieldWithBrowseButton;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileChooser.FileChooserTextBoxBuilder;
import consulo.language.editor.ui.EditorBox;
import consulo.language.editor.ui.awt.TextFieldCompletionProvider;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.rust.localize.RustLocalize;
import consulo.ui.CheckBox;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.util.FormBuilder;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.api.toolchain.BacktraceMode;
import org.rust.cargo.api.toolchain.RustChannel;
import org.rust.cargo.project.model.CargoProjectLocator;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.runconfig.target.BuildTarget;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.toolchain.RsToolchainLocator;
import org.rust.cargo.toolchain.tools.Rustup;
import org.rust.experiments.RsExperiments;
import org.rust.ide.cargo.completion.CargoCommandCompletionProvider;
import org.rust.openapiext.OpenApiUtil;

import java.nio.file.Path;
import java.util.List;

public class CargoCommandConfigurationEditor extends RsCommandConfigurationEditor<CargoCommandConfiguration> {
    @Nullable
    private ComboBox<RustChannel> myChannel;
    @Nullable
    private ComboBox<BacktraceMode> myBacktraceMode;
    @Nullable
    private ComboBox<CargoProject> myCargoProject;
    @Nullable
    private EnvironmentVariablesTextFieldWithBrowseButton myEnvironmentVariables;
    @Nullable
    private CheckBox myRequiredFeatures;
    @Nullable
    private CheckBox myAllFeatures;
    @Nullable
    private CheckBox myWithSudo;
    @Nullable
    private CheckBox myBuildOnRemoteTarget;
    @Nullable
    private CheckBox myRedirectInputEnabled;
    @Nullable
    private FileChooserTextBoxBuilder.Controller myRedirectInput;

    private boolean myRustupAvailable = true;

    public CargoCommandConfigurationEditor(@Nonnull Project project) {
        super(project);
    }

    @Nonnull
    @Override
    protected TextFieldCompletionProvider createCommandCompletionProvider() {
        return new CargoCommandCompletionProvider(CargoProjectServiceUtil.getCargoProjects(project), this::currentWorkspace);
    }

    @Override
    @RequiredUIAccess
    protected Component createForm(EditorBox command, CheckBox emulateTerminal, FileChooserTextBoxBuilder.Controller workingDirectory) {
        ComboBox<RustChannel> channel = ComboBox.create(RustChannel.values());
        channel.setTextRenderer(value -> value == null ? LocalizeValue.empty() : LocalizeValue.of(value.toString()));
        myChannel = channel;

        ComboBox<BacktraceMode> backtraceMode = ComboBox.create(BacktraceMode.values());
        backtraceMode.setTextRenderer(value -> value == null ? LocalizeValue.empty() : LocalizeValue.of(value.toString()));
        myBacktraceMode = backtraceMode;

        List<CargoProject> cargoProjects = CargoProjectServiceUtil.getCargoProjects(project).getAllProjects()
            .stream()
            .sorted((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getPresentableName(), b.getPresentableName()))
            .toList();
        ComboBox<CargoProject> cargoProject = ComboBox.create(cargoProjects);
        cargoProject.setTextRenderer(value -> value == null ? LocalizeValue.empty() : LocalizeValue.of(value.getPresentableName()));
        cargoProject.addValueListener(event -> {
            CargoProject selected = event.getValue();
            if (selected != null) {
                setWorkingDirectory(CargoProjectLocator.getWorkingDirectory(selected));
            }
        });
        myCargoProject = cargoProject;

        EnvironmentVariablesTextFieldWithBrowseButton environmentVariables = new EnvironmentVariablesTextFieldWithBrowseButton();
        myEnvironmentVariables = environmentVariables;

        CheckBox requiredFeatures = CheckBox.create(RustLocalize.checkboxImplicitlyAddRequiredFeaturesIfPossible(), true);
        myRequiredFeatures = requiredFeatures;

        CheckBox allFeatures = CheckBox.create(RustLocalize.checkboxUseAllFeaturesInTests(), false);
        myAllFeatures = allFeatures;

        CheckBox withSudo = CheckBox.create(
            Platform.current().os().isWindows()
                ? RustLocalize.checkboxRunWithAdministratorPrivileges()
                : RustLocalize.checkboxRunWithRootPrivileges(),
            false
        );
        withSudo.setEnabled(OpenApiUtil.isFeatureEnabled(RsExperiments.BUILD_TOOL_WINDOW));
        myWithSudo = withSudo;

        CheckBox buildOnRemoteTarget = CheckBox.create(RustLocalize.checkboxBuildOnRemoteTarget(), true);
        buildOnRemoteTarget.setVisible(false);
        myBuildOnRemoteTarget = buildOnRemoteTarget;

        FileChooserTextBoxBuilder.Controller redirectInput = FileChooserTextBoxBuilder.create(project)
            .fileChooserDescriptor(FileChooserDescriptorFactory.createSingleLocalFileDescriptor())
            .build();
        redirectInput.getComponent().setEnabled(false);
        myRedirectInput = redirectInput;

        CheckBox redirectInputEnabled = CheckBox.create(RustLocalize.checkboxRedirectInputFrom(), false);
        redirectInputEnabled.addValueListener(event -> redirectInput.getComponent().setEnabled(Boolean.TRUE.equals(event.getValue())));
        myRedirectInputEnabled = redirectInputEnabled;

        DockLayout workingDirectoryRow = DockLayout.create(Space.SMALL).center(workingDirectory.getComponent());
        if (cargoProjects.size() > 1) {
            workingDirectoryRow.right(cargoProject);
        }

        return FormBuilder.create()
            .addLabeled(
                RustLocalize.command(),
                DockLayout.create(Space.SMALL)
                    .center(command)
                    .right(HorizontalLayout.create(Space.SMALL).add(Label.create(RustLocalize.labelChannel())).add(channel))
            )
            .addLabeled(LocalizeValue.empty(), requiredFeatures)
            .addLabeled(LocalizeValue.empty(), allFeatures)
            .addLabeled(LocalizeValue.empty(), emulateTerminal)
            .addLabeled(LocalizeValue.empty(), withSudo)
            .addLabeled(LocalizeValue.empty(), buildOnRemoteTarget)
            .addLabeled(
                LocalizeValue.join(ExecutionLocalize.environmentVariablesComponentTitle(), LocalizeValue.colon()),
                environmentVariables.getComponent()
            )
            .addLabeled(ExecutionLocalize.runConfigurationWorkingDirectoryLabel(), workingDirectoryRow)
            .addLabeled(redirectInputEnabled, redirectInput.getComponent())
            .addLabeled(RustLocalize.backtrace(), backtraceMode)
            .build();
    }

    @Override
    @RequiredUIAccess
    protected void resetEditorFrom(@Nonnull CargoCommandConfiguration configuration) {
        super.resetEditorFrom(configuration);

        ComboBox<RustChannel> channel = myChannel;
        ComboBox<BacktraceMode> backtraceMode = myBacktraceMode;
        ComboBox<CargoProject> cargoProject = myCargoProject;
        EnvironmentVariablesTextFieldWithBrowseButton environmentVariables = myEnvironmentVariables;
        CheckBox requiredFeatures = myRequiredFeatures;
        CheckBox allFeatures = myAllFeatures;
        CheckBox withSudo = myWithSudo;
        CheckBox buildOnRemoteTarget = myBuildOnRemoteTarget;
        CheckBox redirectInputEnabled = myRedirectInputEnabled;
        FileChooserTextBoxBuilder.Controller redirectInput = myRedirectInput;
        if (channel == null || backtraceMode == null || cargoProject == null || environmentVariables == null
            || requiredFeatures == null || allFeatures == null || withSudo == null || buildOnRemoteTarget == null
            || redirectInputEnabled == null || redirectInput == null) {
            return;
        }

        RsToolchainBase toolchain = RsToolchainLocator.getToolchain(project);
        myRustupAvailable = toolchain != null && Rustup.isRustupAvailable(toolchain);

        channel.setValue(configuration.getChannel());
        channel.setEnabled(myRustupAvailable || configuration.getChannel() != RustChannel.DEFAULT);
        requiredFeatures.setValue(configuration.getRequiredFeatures());
        allFeatures.setValue(configuration.getAllFeatures());
        withSudo.setValue(configuration.getWithSudo());
        buildOnRemoteTarget.setValue(configuration.getBuildTarget().isRemote());
        backtraceMode.setValue(configuration.getBacktrace());
        environmentVariables.setData(configuration.getEnv());

        Path workingDirectory = getCurrentWorkingDirectory();
        VirtualFile file = workingDirectory == null ? null : LocalFileSystem.getInstance().findFileByIoFile(workingDirectory.toFile());
        CargoProject projectForWorkingDirectory =
            file == null ? null : CargoProjectServiceUtil.getCargoProjects(project).findProjectForFile(file);
        if (projectForWorkingDirectory != null) {
            cargoProject.setValue(projectForWorkingDirectory, false);
        }

        redirectInputEnabled.setValue(configuration.isRedirectInput());
        redirectInput.getComponent().setEnabled(configuration.isRedirectInput());
        String redirectPath = configuration.getRedirectInputPath();
        redirectInput.setValue(redirectPath != null ? redirectPath : "");
    }

    @Override
    protected void applyEditorTo(@Nonnull CargoCommandConfiguration configuration) throws ConfigurationException {
        super.applyEditorTo(configuration);

        ComboBox<RustChannel> channel = myChannel;
        ComboBox<BacktraceMode> backtraceMode = myBacktraceMode;
        EnvironmentVariablesTextFieldWithBrowseButton environmentVariables = myEnvironmentVariables;
        CheckBox requiredFeatures = myRequiredFeatures;
        CheckBox allFeatures = myAllFeatures;
        CheckBox withSudo = myWithSudo;
        CheckBox buildOnRemoteTarget = myBuildOnRemoteTarget;
        CheckBox redirectInputEnabled = myRedirectInputEnabled;
        FileChooserTextBoxBuilder.Controller redirectInput = myRedirectInput;
        if (channel == null || backtraceMode == null || environmentVariables == null || requiredFeatures == null
            || allFeatures == null || withSudo == null || buildOnRemoteTarget == null || redirectInputEnabled == null
            || redirectInput == null) {
            return;
        }

        RustChannel selectedChannel = channel.getValue() == null ? RustChannel.DEFAULT : channel.getValue();
        configuration.setChannel(selectedChannel);
        configuration.setRequiredFeatures(requiredFeatures.getValueOrError());
        configuration.setAllFeatures(allFeatures.getValueOrError());
        configuration.setWithSudo(withSudo.getValueOrError());
        configuration.setBuildTarget(buildOnRemoteTarget.getValueOrError() ? BuildTarget.REMOTE : BuildTarget.LOCAL);
        configuration.setBacktrace(backtraceMode.getValue() == null ? BacktraceMode.DEFAULT : backtraceMode.getValue());
        configuration.setEnv(environmentVariables.getData());

        configuration.setRedirectInput(redirectInputEnabled.getValueOrError());
        String redirectPath = redirectInput.getValue();
        configuration.setRedirectInputPath(
            redirectPath != null && !redirectPath.isEmpty() ? FileUtil.toSystemIndependentName(redirectPath) : null
        );

        if (!myRustupAvailable && selectedChannel != RustChannel.DEFAULT) {
            throw new ConfigurationException(RustLocalize.dialogMessageChannelCannotBeSetExplicitlyBecauseRustupNotAvailable());
        }
    }
}
