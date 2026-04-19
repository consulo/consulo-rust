/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions.runAnything.cargo;

import com.intellij.execution.Executor;
import com.intellij.ide.actions.runAnything.items.RunAnythingItem;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import org.rust.RsBundle;
import org.rust.cargo.icons.CargoIcons;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.runconfig.RunConfigUtil;
import org.rust.cargo.toolchain.CargoCommandLine;
import org.rust.cargo.util.CargoCommandCompletionProvider;
import org.rust.cargo.util.RsCommandCompletionProvider;
import org.rust.ide.actions.runAnything.RsRunAnythingProvider;

import javax.swing.*;
import java.nio.file.Path;
import java.util.List;

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
    public Icon getIcon(String value) {
        return CargoIcons.ICON;
    }

    @Override
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
    public Icon getHelpIcon() {
        return CargoIcons.ICON;
    }

    @Override
    public String getHelpDescription() {
        return RsBundle.message("runs.cargo.command");
    }
}
