/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions.runAnything.cargo;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.executor.Executor;
import consulo.ide.runAnything.RunAnythingItem;
import consulo.dataContext.DataContext;
import consulo.project.Project;
import org.rust.RsBundle;
import org.rust.cargo.icons.CargoIcons;
import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.runconfig.RunConfigUtil;
import org.rust.cargo.toolchain.CargoCommandLine;
import org.rust.ide.cargo.completion.CargoCommandCompletionProvider;
import org.rust.ide.cargo.completion.RsCommandCompletionProvider;
import org.rust.ide.actions.runAnything.RsRunAnythingProvider;

import javax.swing.*;
import java.nio.file.Path;
import java.util.List;
import consulo.ui.image.Image;

@ExtensionImpl(order = "after RunAnythingRunConfigurationProviderImpl")
public class CargoRunAnythingProvider extends RsRunAnythingProvider {

    public static final String HELP_COMMAND = "cargo";

    @Override
    public RunAnythingItem getMainListItem(DataContext dataContext, String value) {
        return new RunAnythingCargoItem(getCommand(value), getIcon(value));
    }

    @Override
    protected void run(Executor executor, String command, List<String> params, Path workingDirectory, CargoProject cargoProject) {
        new CargoCommandLine(command, workingDirectory, params).run(cargoProject, command, true, executor);
    }

    @Override
    public RsCommandCompletionProvider getCompletionProvider(Project project, DataContext dataContext) {
        return new CargoCommandCompletionProvider(CargoProjectServiceUtil.getCargoProjects(project), () ->
            RunConfigUtil.getAppropriateCargoProject(dataContext) != null
                ? RunConfigUtil.getAppropriateCargoProject(dataContext).getWorkspace()
                : null
        );
    }

    @Override
    public String getCommand(String value) {
        return value;
    }

    @Override
    public consulo.ui.image.Image getIcon(String value) {
        return CargoIcons.ICON;
    }

    public String getCompletionGroupTitle() {
        return RsBundle.message("cargo.commands");
    }

    @Override
    public String getHelpGroupTitle() {
        return RsBundle.message("build.event.title.cargo");
    }

    @Override
    public String getHelpCommandPlaceholder() {
        return "cargo <subcommand> <args...>";
    }

    @Override
    public String getHelpCommand() {
        return HELP_COMMAND;
    }

    @Override
    public consulo.ui.image.Image getHelpIcon() {
        return CargoIcons.ICON;
    }

    @Override
    public String getHelpDescription() {
        return RsBundle.message("runs.cargo.command");
    }
}
