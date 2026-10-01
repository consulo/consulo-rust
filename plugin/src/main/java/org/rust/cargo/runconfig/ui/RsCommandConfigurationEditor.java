/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.ui;

import consulo.configurable.ConfigurationException;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileChooser.FileChooserTextBoxBuilder;
import consulo.language.editor.ui.EditorBox;
import consulo.language.editor.ui.EditorBoxBuilderFactory;
import consulo.language.editor.ui.awt.TextFieldCompletionProvider;
import consulo.project.Project;
import consulo.rust.localize.RustLocalize;
import consulo.ui.CheckBox;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.api.workspace.CargoWorkspace;
import org.rust.cargo.project.model.CargoProjectLocator;
import org.rust.cargo.runconfig.RsCommandConfiguration;

import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class RsCommandConfigurationEditor<T extends RsCommandConfiguration> extends SettingsEditor<T> {
    @Nonnull
    protected final Project project;

    @Nullable
    private EditorBox myCommand;
    @Nullable
    private CheckBox myEmulateTerminal;
    @Nullable
    private FileChooserTextBoxBuilder.Controller myWorkingDirectory;

    protected RsCommandConfigurationEditor(@Nonnull Project project) {
        this.project = project;
    }

    @Nonnull
    protected abstract TextFieldCompletionProvider createCommandCompletionProvider();

    @RequiredUIAccess
    protected abstract Component createForm(
        EditorBox command,
        CheckBox emulateTerminal,
        FileChooserTextBoxBuilder.Controller workingDirectory
    );

    @Override
    @RequiredUIAccess
    protected Component createUIComponent() {
        EditorBox command = project.getApplication()
            .getInstance(EditorBoxBuilderFactory.class)
            .create(project)
            .completion(createCommandCompletionProvider())
            .build();
        myCommand = command;

        CheckBox emulateTerminal =
            CheckBox.create(RustLocalize.checkboxEmulateTerminalInOutputConsole(), RsCommandConfiguration.getEmulateTerminalDefault());
        myEmulateTerminal = emulateTerminal;

        FileChooserTextBoxBuilder.Controller workingDirectory = FileChooserTextBoxBuilder.create(project)
            .fileChooserDescriptor(FileChooserDescriptorFactory.createSingleFolderDescriptor())
            .build();
        myWorkingDirectory = workingDirectory;

        return createForm(command, emulateTerminal, workingDirectory);
    }

    @Nullable
    protected CargoWorkspace currentWorkspace() {
        EditorBox command = myCommand;
        CargoProject cargoProject = CargoProjectLocator.findCargoProject(
            project,
            command == null ? "" : command.getValue(),
            getCurrentWorkingDirectory()
        );
        return cargoProject == null ? null : cargoProject.getWorkspace();
    }

    @Nullable
    protected Path getCurrentWorkingDirectory() {
        FileChooserTextBoxBuilder.Controller workingDirectory = myWorkingDirectory;
        String text = workingDirectory == null ? null : workingDirectory.getValue();
        if (text == null || text.isEmpty()) {
            return null;
        }
        return Paths.get(text);
    }

    @RequiredUIAccess
    protected void setWorkingDirectory(@Nullable Path path) {
        FileChooserTextBoxBuilder.Controller workingDirectory = myWorkingDirectory;
        if (workingDirectory != null) {
            workingDirectory.setValue(path != null ? path.toString() : "");
        }
    }

    @Override
    @RequiredUIAccess
    protected void resetEditorFrom(@Nonnull T configuration) {
        EditorBox command = myCommand;
        CheckBox emulateTerminal = myEmulateTerminal;
        if (command == null || emulateTerminal == null) {
            return;
        }

        command.setValue(configuration.getCommand());
        setWorkingDirectory(configuration.getWorkingDirectory());
        emulateTerminal.setValue(configuration.getEmulateTerminal());
    }

    @Override
    protected void applyEditorTo(@Nonnull T configuration) throws ConfigurationException {
        EditorBox command = myCommand;
        CheckBox emulateTerminal = myEmulateTerminal;
        if (command == null || emulateTerminal == null) {
            return;
        }

        configuration.setCommand(command.getValue());
        configuration.setWorkingDirectory(getCurrentWorkingDirectory());
        configuration.setEmulateTerminal(emulateTerminal.getValueOrError());
    }
}
