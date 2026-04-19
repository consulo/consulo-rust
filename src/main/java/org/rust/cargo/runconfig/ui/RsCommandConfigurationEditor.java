/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.ui;

import com.intellij.execution.ExecutionBundle;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBCheckBox;
// // import com.intellij.util.text.StringUtilRt; // REMOVED // REMOVED
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.runconfig.RsCommandConfiguration;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.util.RsCommandLineEditor;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class RsCommandConfigurationEditor<T extends RsCommandConfiguration> extends SettingsEditor<T> {

    @NotNull
    protected final Project project;

    @NotNull
    public abstract RsCommandLineEditor getCommand();

    protected final JCheckBox emulateTerminal =
        new JBCheckBox(RsBundle.message("checkbox.emulate.terminal.in.output.console"), RsCommandConfiguration.getEmulateTerminalDefault());

    protected RsCommandConfigurationEditor(@NotNull Project project) {
        this.project = project;
    }

    @Nullable
    protected CargoWorkspace currentWorkspace() {
        return CargoCommandConfiguration.findCargoProject(project, getCommand().getText(), getCurrentWorkingDirectory()) != null
            ? CargoCommandConfiguration.findCargoProject(project, getCommand().getText(), getCurrentWorkingDirectory()).getWorkspace()
            : null;
    }

    @Nullable
    protected Path getCurrentWorkingDirectory() {
        String text = workingDirectory.getComponent().getText();
        if (text == null || text.isEmpty()) return null;
        return Paths.get(text);
    }

    protected final LabeledComponent<TextFieldWithBrowseButton> workingDirectory = createWorkingDirectoryComponent();

    @Override
    protected void resetEditorFrom(@NotNull T configuration) {
        getCommand().setText(configuration.getCommand());
        Path wd = configuration.getWorkingDirectory();
        workingDirectory.getComponent().setText(wd != null ? wd.toString() : "");
        emulateTerminal.setSelected(configuration.getEmulateTerminal());
    }

    @Override
    protected void applyEditorTo(@NotNull T configuration) throws com.intellij.openapi.options.ConfigurationException {
        configuration.setCommand(getCommand().getText());
        configuration.setWorkingDirectory(getCurrentWorkingDirectory());
        configuration.setEmulateTerminal(emulateTerminal.isSelected());
    }

    @NotNull
    private static LabeledComponent<TextFieldWithBrowseButton> createWorkingDirectoryComponent() {
        LabeledComponent<TextFieldWithBrowseButton> component = new LabeledComponent<>();
        TextFieldWithBrowseButton textField = new TextFieldWithBrowseButton();
        textField.addBrowseFolderListener(
            null, null, null,
            FileChooserDescriptorFactory.createSingleFolderDescriptor()
        );
        component.setComponent(textField);
        component.setText(ExecutionBundle.message("run.configuration.working.directory.label"));
        return component;
    }
}
