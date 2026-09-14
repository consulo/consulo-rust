/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.ui;

import consulo.application.ApplicationManager;
import consulo.configurable.ConfigurationException;
import consulo.execution.ExecutionBundle;
import consulo.execution.ui.awt.EnvironmentVariablesComponent;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.project.Project;
import consulo.ui.ex.awt.*;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.api.toolchain.BacktraceMode;
import org.rust.cargo.api.toolchain.RustChannel;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.runconfig.target.BuildTarget;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.toolchain.RsToolchainLocator;
import org.rust.cargo.toolchain.tools.Rustup;
import org.rust.cargo.util.RsCommandLineEditor;
import org.rust.experiments.RsExperiments;
import org.rust.ide.cargo.completion.CargoCommandCompletionProvider;
import org.rust.openapiext.OpenApiUtil;
import org.rust.openapiext.ui.UiDslUtil;

import javax.swing.*;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class CargoCommandConfigurationEditor extends RsCommandConfigurationEditor<CargoCommandConfiguration> {
    private JComponent panel;

    private boolean isRemoteTarget() {
        return false; // RUN_ON_TARGET_NAME_KEY isn't available in Consulo
    }

    @Nonnull
    private final RsCommandLineEditor command;

    @Nonnull
    @Override
    public RsCommandLineEditor getCommand() {
        return command;
    }

    private final List<CargoProject> allCargoProjects;

    private final ComboBox<BacktraceMode> backtraceMode = new ComboBox<>();
    private final JBLabel channelLabel = new JBLabel(RsBundle.message("label.channel"));
    private final ComboBox<RustChannel> channel = new ComboBox<>();

    private final ComboBox<CargoProject> cargoProject = new ComboBox<>();

    private final TextFieldWithBrowseButton redirectInput;
    private final JCheckBox isRedirectInput;

    private final EnvironmentVariablesComponent environmentVariables = new EnvironmentVariablesComponent();
    private final JCheckBox requiredFeatures = new JBCheckBox(RsBundle.message("checkbox.implicitly.add.required.features.if.possible"), true);
    private final JCheckBox allFeatures = new JBCheckBox(RsBundle.message("checkbox.use.all.features.in.tests"), false);
    private final JCheckBox withSudo;
    private final JCheckBox buildOnRemoteTarget = new JBCheckBox(RsBundle.message("checkbox.build.on.remote.target"), true);

    public CargoCommandConfigurationEditor(@Nonnull Project project) {
        super(project);
        this.command = new RsCommandLineEditor(
            project, new CargoCommandCompletionProvider(CargoProjectServiceUtil.getCargoProjects(project), () -> currentWorkspace())
        );

        allCargoProjects = CargoProjectServiceUtil.getCargoProjects(project).getAllProjects()
            .stream()
            .sorted((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getPresentableName(), b.getPresentableName()))
            .collect(Collectors.toList());

        for (BacktraceMode mode : BacktraceMode.values()) {
            backtraceMode.addItem(mode);
        }

        for (RustChannel ch : RustChannel.values()) {
            channel.addItem(ch);
        }

        cargoProject.setRenderer(SimpleListCellRenderer.create("", CargoProject::getPresentableName));
        for (CargoProject cp : allCargoProjects) {
            cargoProject.addItem(cp);
        }
        cargoProject.addItemListener(e -> setWorkingDirectoryFromSelectedProject());

        redirectInput = UiDslUtil.pathTextField(FileChooserDescriptorFactory.createSingleLocalFileDescriptor(), this, "");
        redirectInput.setEnabled(false);

        isRedirectInput = new JBCheckBox(ExecutionBundle.message("redirect.input.from"), false);
        isRedirectInput.addChangeListener(e -> redirectInput.setEnabled(isRedirectInput.isSelected()));

        String sudoLabel = consulo.platform.Platform.current().os().isWindows()
            ? RsBundle.message("checkbox.run.with.administrator.privileges")
            : RsBundle.message("checkbox.run.with.root.privileges");
        withSudo = new JBCheckBox(sudoLabel, false);
        // TODO: remove once elevated execution supports error stream redirection
        withSudo.setEnabled(OpenApiUtil.isFeatureEnabled(RsExperiments.BUILD_TOOL_WINDOW));
    }

    private void setWorkingDirectoryFromSelectedProject() {
        int idx = cargoProject.getSelectedIndex();
        if (idx == -1) {
            return;
        }
        CargoProject selectedProject = cargoProject.getItemAt(idx);
        Path wd = org.rust.cargo.project.model.CargoProjectLocator.getWorkingDirectory(selectedProject);
        workingDirectory.getComponent().setText(wd != null ? wd.toString() : "");
    }

    @Override
    protected void resetEditorFrom(@Nonnull CargoCommandConfiguration configuration) {
        super.resetEditorFrom(configuration);

        channel.setSelectedIndex(configuration.getChannel().getIndex());
        requiredFeatures.setSelected(configuration.getRequiredFeatures());
        allFeatures.setSelected(configuration.getAllFeatures());
        withSudo.setSelected(configuration.getWithSudo());
        buildOnRemoteTarget.setSelected(configuration.getBuildTarget().isRemote());
        backtraceMode.setSelectedIndex(configuration.getBacktrace().getIndex());
        environmentVariables.setEnvData(configuration.getEnv());

        Path cwd = getCurrentWorkingDirectory();
        VirtualFile vFile = cwd != null
            ? LocalFileSystem.getInstance().findFileByIoFile(cwd.toFile())
            : null;
        if (vFile == null) {
            cargoProject.setSelectedIndex(-1);
        }
        else {
            CargoProject projectForWd = CargoProjectServiceUtil.getCargoProjects(project).findProjectForFile(vFile);
            cargoProject.setSelectedIndex(allCargoProjects.indexOf(projectForWd));
        }

        isRedirectInput.setSelected(configuration.isRedirectInput());
        String redirectPath = configuration.getRedirectInputPath();
        redirectInput.setText(redirectPath != null ? redirectPath : "");

        hideUnsupportedFieldsIfNeeded();
    }

    @Override
    protected void applyEditorTo(@Nonnull CargoCommandConfiguration configuration) throws ConfigurationException {
        super.applyEditorTo(configuration);

        RustChannel configChannel = RustChannel.fromIndex(channel.getSelectedIndex());

        configuration.setChannel(configChannel);
        configuration.setRequiredFeatures(requiredFeatures.isSelected());
        configuration.setAllFeatures(allFeatures.isSelected());
        configuration.setWithSudo(withSudo.isSelected());
        configuration.setBuildTarget(buildOnRemoteTarget.isSelected() ? BuildTarget.REMOTE : BuildTarget.LOCAL);
        configuration.setBacktrace(BacktraceMode.fromIndex(backtraceMode.getSelectedIndex()));
        configuration.setEnv(environmentVariables.getEnvData());

        RsToolchainBase toolchain = RsToolchainLocator.getToolchain(project);

        boolean rustupAvailable = toolchain != null && Rustup.isRustupAvailable(toolchain);
        channel.setEnabled(rustupAvailable || configChannel != RustChannel.DEFAULT);
        if (!rustupAvailable && configChannel != RustChannel.DEFAULT) {
            throw new ConfigurationException(RsBundle.message("dialog.message.channel.cannot.be.set.explicitly.because.rustup.not.available"));
        }

        configuration.setRedirectInput(isRedirectInput.isSelected());
        String redirectPath = redirectInput.getText();
        configuration.setRedirectInputPath(
            redirectPath != null && !redirectPath.isEmpty()
                ? FileUtil.toSystemIndependentName(redirectPath)
                : null
        );

        hideUnsupportedFieldsIfNeeded();
    }

    @Nonnull
    @Override
    protected JComponent createEditor() {
        // form using {@link consulo.ui.ex.awt.FormBuilder}. The label column comes from
        // the `LabeledComponent` widgets, so we leave the label parameter empty.
        consulo.ui.ex.awt.FormBuilder builder = consulo.ui.ex.awt.FormBuilder.createFormBuilder();

        // Command row: command field + channel label/combo on the right.
        JPanel commandRow = new JPanel(new java.awt.BorderLayout(8, 0));
        commandRow.add(command, java.awt.BorderLayout.CENTER);
        JPanel channelGroup = new JPanel();
        channelLabel.setLabelFor(channel);
        channelGroup.add(channelLabel);
        channelGroup.add(channel);
        commandRow.add(channelGroup, java.awt.BorderLayout.EAST);
        builder.addLabeledComponent(RsBundle.message("command"), commandRow);

        builder.addComponent(requiredFeatures);
        builder.addComponent(allFeatures);
        builder.addComponent(emulateTerminal);
        builder.addComponent(withSudo);
        builder.addComponent(buildOnRemoteTarget);

        builder.addLabeledComponent(environmentVariables.getLabel(), environmentVariables.getComponent());

        JPanel workingDirRow = new JPanel(new java.awt.BorderLayout(8, 0));
        workingDirRow.add(workingDirectory.getComponent(), java.awt.BorderLayout.CENTER);
        if (CargoProjectServiceUtil.getCargoProjects(project).getAllProjects().size() > 1) {
            workingDirRow.add(cargoProject, java.awt.BorderLayout.EAST);
        }
        builder.addLabeledComponent(workingDirectory.getLabel(), workingDirRow);

        JPanel redirectRow = new JPanel(new java.awt.BorderLayout(8, 0));
        redirectRow.add(isRedirectInput, java.awt.BorderLayout.WEST);
        redirectRow.add(redirectInput, java.awt.BorderLayout.CENTER);
        builder.addComponent(redirectRow);

        builder.addLabeledComponent(RsBundle.message("backtrace"), backtraceMode);

        JComponent result = builder.getPanel();
        panel = result;
        return result;
    }

    private void hideUnsupportedFieldsIfNeeded() {
        if (!ApplicationManager.getApplication().isDispatchThread()) {
            return;
        }
        buildOnRemoteTarget.setVisible(isRemoteTarget());
    }
}
