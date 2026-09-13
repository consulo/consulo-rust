/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.ui;

import consulo.execution.ExecutionBundle;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.project.Project;
import consulo.ui.ex.awt.LabeledComponent;
import consulo.ui.ex.awt.TextFieldWithBrowseButton;
import consulo.ui.ex.awt.JBCheckBox;
// // import consulo.util.nodep.text.StringUtilRt; // REMOVED // REMOVED
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.api.workspace.CargoWorkspace;
import org.rust.cargo.runconfig.RsCommandConfiguration;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.util.RsCommandLineEditor;

import javax.swing.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import consulo.configurable.ConfigurationException;

public abstract class RsCommandConfigurationEditor<T extends RsCommandConfiguration> extends SettingsEditor<T> {

    @Nonnull
    protected final Project project;

    @Nonnull
    public abstract RsCommandLineEditor getCommand();

    protected final JCheckBox emulateTerminal =
        new JBCheckBox(RsBundle.message("checkbox.emulate.terminal.in.output.console"), RsCommandConfiguration.getEmulateTerminalDefault());

    protected RsCommandConfigurationEditor(@Nonnull Project project) {
        this.project = project;
    }

    @Nullable
    protected CargoWorkspace currentWorkspace() {
        return org.rust.cargo.project.model.CargoProjectLocator.findCargoProject(project, getCommand().getText(), getCurrentWorkingDirectory()) != null
            ? org.rust.cargo.project.model.CargoProjectLocator.findCargoProject(project, getCommand().getText(), getCurrentWorkingDirectory()).getWorkspace()
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
    protected void resetEditorFrom(@Nonnull T configuration) {
        getCommand().setText(configuration.getCommand());
        Path wd = configuration.getWorkingDirectory();
        workingDirectory.getComponent().setText(wd != null ? wd.toString() : "");
        emulateTerminal.setSelected(configuration.getEmulateTerminal());
    }

    @Override
    protected void applyEditorTo(@Nonnull T configuration) throws consulo.configurable.ConfigurationException {
        configuration.setCommand(getCommand().getText());
        configuration.setWorkingDirectory(getCurrentWorkingDirectory());
        configuration.setEmulateTerminal(emulateTerminal.isSelected());
    }

    @Nonnull
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
