/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.ui;

import consulo.disposer.Disposable;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.configuration.fragment.RunConfigurationFragmentEditor;
import consulo.execution.configuration.fragment.RunConfigurationFragmentLayout;
import consulo.execution.configuration.fragment.StandardRunConfigurationFragmentOrder;
import consulo.execution.localize.ExecutionLocalize;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileChooser.FileChooserTextBoxBuilder;
import consulo.language.editor.ui.EditorBox;
import consulo.language.editor.ui.EditorBoxBuilderFactory;
import consulo.language.editor.ui.awt.TextFieldCompletionProvider;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.rust.localize.RustLocalize;
import consulo.ui.ComboBox;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;
import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.api.model.CargoProjectsService;
import org.rust.cargo.api.workspace.CargoWorkspace;
import org.rust.cargo.project.model.CargoProjectLocator;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.runconfig.RsCommandConfiguration;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Supplier;

/**
 * The command line of a cargo-like tool and the directory it runs in.
 */
public abstract class RsCommandFragment<T extends RsCommandConfiguration> implements RunConfigurationFragment<T> {
    @Override
    public String getId() {
        return "rustCommand";
    }

    @Override
    public LocalizeValue getName() {
        return RustLocalize.runConfigurationFragmentCommand();
    }

    @Override
    public int getOrder() {
        return StandardRunConfigurationFragmentOrder.FIRST;
    }

    @Override
    public boolean isRemovable() {
        return false;
    }

    protected abstract TextFieldCompletionProvider createCompletionProvider(CargoProjectsService projects,
                                                                            Supplier<@Nullable CargoWorkspace> workspace);

    @RequiredUIAccess
    @Override
    public RunConfigurationFragmentEditor<T> createEditor(Project project, Disposable parent) {
        CargoProjectsService cargoProjects = CargoProjectServiceUtil.getCargoProjects(project);

        FileChooserTextBoxBuilder.Controller workingDirectory = FileChooserTextBoxBuilder.create(project)
            .fileChooserDescriptor(FileChooserDescriptorFactory.createSingleFolderDescriptor())
            .uiDisposable(parent)
            .build();

        EditorBox[] commandRef = new EditorBox[1];
        EditorBox command = project.getApplication()
            .getInstance(EditorBoxBuilderFactory.class)
            .create(project)
            .completion(createCompletionProvider(cargoProjects, () -> {
                CargoProject cargoProject = CargoProjectLocator.findCargoProject(
                    project,
                    commandRef[0] == null ? "" : commandRef[0].getValue(),
                    toPath(workingDirectory.getValue())
                );
                return cargoProject == null ? null : cargoProject.getWorkspace();
            }))
            .build();
        commandRef[0] = command;

        List<CargoProject> projects = cargoProjects.getAllProjects()
            .stream()
            .sorted((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(a.getPresentableName(), b.getPresentableName()))
            .toList();
        ComboBox<CargoProject> cargoProject = ComboBox.create(projects);
        cargoProject.setTextRenderer(value -> value == null ? LocalizeValue.empty() : LocalizeValue.of(value.getPresentableName()));
        cargoProject.addValueListener(event -> {
            CargoProject selected = event.getValue();
            if (selected != null) {
                workingDirectory.setValue(CargoProjectLocator.getWorkingDirectory(selected).toString());
            }
        });

        DockLayout workingDirectoryRow = DockLayout.create(Space.SMALL).center(workingDirectory.getComponent());
        if (projects.size() > 1) {
            workingDirectoryRow.right(cargoProject);
        }

        return new RunConfigurationFragmentEditor<>() {
            @RequiredUIAccess
            @Override
            public void addRows(RunConfigurationFragmentLayout layout) {
                layout.addLabeled(RustLocalize.command(), command);
                layout.addLabeled(ExecutionLocalize.runConfigurationWorkingDirectoryLabel(), workingDirectoryRow);
            }

            @RequiredUIAccess
            @Override
            public void reset(T target) {
                command.setValue(target.getCommand());
                Path path = target.getWorkingDirectory();
                workingDirectory.setValue(path == null ? "" : path.toString(), false);

                VirtualFile file = path == null ? null : LocalFileSystem.getInstance().findFileByNioFile(path);
                CargoProject projectForDirectory = file == null ? null : cargoProjects.findProjectForFile(file);
                if (projectForDirectory != null) {
                    cargoProject.setValue(projectForDirectory, false);
                }
            }

            @RequiredUIAccess
            @Override
            public void apply(T target) {
                target.setCommand(command.getValue());
                target.setWorkingDirectory(toPath(workingDirectory.getValue()));
            }
        };
    }

    private static @Nullable Path toPath(@Nullable String text) {
        return StringUtil.isEmptyOrSpaces(text) ? null : Paths.get(text.trim());
    }
}
