/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.ui;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.configuration.EnvironmentVariablesComponent;
import com.intellij.execution.impl.SingleConfigurationConfigurable;
import com.intellij.ide.DataManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.dsl.builder.RowLayout;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.runconfig.target.BuildTarget;
import org.rust.cargo.toolchain.BacktraceMode;
import org.rust.cargo.toolchain.RustChannel;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.toolchain.tools.Rustup;
import org.rust.cargo.toolchain.wsl.RsWslToolchain;
import org.rust.cargo.util.CargoCommandCompletionProvider;
import org.rust.cargo.util.RsCommandLineEditor;
import org.rust.ide.experiments.RsExperiments;
import org.rust.openapiext.UiDslUtil;
import org.rust.openapiext.OpenApiUtil;

import javax.swing.*;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class CargoCommandConfigurationEditor extends RsCommandConfigurationEditor<CargoCommandConfiguration> {
    private JComponent panel;

    private boolean isRemoteTarget() {
        return DataManager.getInstance().getDataContext(panel).getData(SingleConfigurationConfigurable.RUN_ON_TARGET_NAME_KEY) != null;
    }

    @NotNull
    private final RsCommandLineEditor command;

    @NotNull
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

    public CargoCommandConfigurationEditor(@NotNull Project project) {
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

        redirectInput = UiDslUtil.pathTextField(FileChooserDescriptorFactory.createSingleFileDescriptor(), this, "");
        redirectInput.setEnabled(false);

        isRedirectInput = new JBCheckBox(ExecutionBundle.message("redirect.input.from"), false);
        isRedirectInput.addChangeListener(e -> redirectInput.setEnabled(isRedirectInput.isSelected()));

        String sudoLabel = SystemInfo.isWindows
            ? RsBundle.message("checkbox.run.with.administrator.privileges")
            : RsBundle.message("checkbox.run.with.root.privileges");
        withSudo = new JBCheckBox(sudoLabel, false);
        // TODO: remove when `com.intellij.execution.process.ElevationService` supports error stream redirection
        withSudo.setEnabled(OpenApiUtil.isFeatureEnabled(RsExperiments.BUILD_TOOL_WINDOW));
    }

    private void setWorkingDirectoryFromSelectedProject() {
        int idx = cargoProject.getSelectedIndex();
        if (idx == -1) return;
        CargoProject selectedProject = cargoProject.getItemAt(idx);
        Path wd = CargoCommandConfiguration.getWorkingDirectory(selectedProject);
        workingDirectory.getComponent().setText(wd != null ? wd.toString() : "");
    }

    @Override
    protected void resetEditorFrom(@NotNull CargoCommandConfiguration configuration) {
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
        } else {
            CargoProject projectForWd = CargoProjectServiceUtil.getCargoProjects(project).findProjectForFile(vFile);
            cargoProject.setSelectedIndex(allCargoProjects.indexOf(projectForWd));
        }

        isRedirectInput.setSelected(configuration.isRedirectInput());
        String redirectPath = configuration.getRedirectInputPath();
        redirectInput.setText(redirectPath != null ? redirectPath : "");

        hideUnsupportedFieldsIfNeeded();
    }

    @Override
    protected void applyEditorTo(@NotNull CargoCommandConfiguration configuration) throws ConfigurationException {
        super.applyEditorTo(configuration);

        RustChannel configChannel = RustChannel.fromIndex(channel.getSelectedIndex());

        configuration.setChannel(configChannel);
        configuration.setRequiredFeatures(requiredFeatures.isSelected());
        configuration.setAllFeatures(allFeatures.isSelected());
        configuration.setWithSudo(withSudo.isSelected());
        configuration.setBuildTarget(buildOnRemoteTarget.isSelected() ? BuildTarget.REMOTE : BuildTarget.LOCAL);
        configuration.setBacktrace(BacktraceMode.fromIndex(backtraceMode.getSelectedIndex()));
        configuration.setEnv(environmentVariables.getEnvData());

        RsToolchainBase toolchain = RsProjectSettingsServiceUtil.getToolchain(project);
        if (toolchain instanceof RsWslToolchain && isRemoteTarget()) {
            throw new ConfigurationException(RsBundle.message("dialog.message.run.targets.cannot.be.used.alongside.with.wsl.toolchain"));
        }

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

    @NotNull
    @Override
    protected JComponent createEditor() {
        // form using {@link com.intellij.util.ui.FormBuilder}. The label column comes from
        // the `LabeledComponent` widgets, so we leave the label parameter empty.
        com.intellij.util.ui.FormBuilder builder = com.intellij.util.ui.FormBuilder.createFormBuilder();

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
        if (!ApplicationManager.getApplication().isDispatchThread()) return;
        buildOnRemoteTarget.setVisible(isRemoteTarget());
    }
}
