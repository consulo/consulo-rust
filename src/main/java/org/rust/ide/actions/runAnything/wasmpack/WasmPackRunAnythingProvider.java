/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions.runAnything.wasmpack;

import com.intellij.execution.Executor;
import com.intellij.ide.actions.runAnything.items.RunAnythingItem;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import org.rust.RsBundle;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.runconfig.RunConfigUtil;
import org.rust.cargo.runconfig.wasmpack.util.WasmPackCommandCompletionProvider;
import org.rust.cargo.toolchain.WasmPackCommandLine;
import org.rust.cargo.util.RsCommandCompletionProvider;
import org.rust.ide.actions.runAnything.RsRunAnythingProvider;
import org.rust.ide.icons.RsIcons;

import javax.swing.*;
import java.nio.file.Path;
import java.util.List;

public class WasmPackRunAnythingProvider extends RsRunAnythingProvider {

    public static final String HELP_COMMAND = "wasm-pack";

    @Override
    public RunAnythingItem getMainListItem(DataContext dataContext, String value) {
        return new RunAnythingWasmPackItem(getCommand(value), getIcon(value));
    }

    @Override
    protected void run(Executor executor, String command, List<String> params, Path workingDirectory, CargoProject cargoProject) {
        new WasmPackCommandLine(command, workingDirectory, params, false).run(cargoProject, command, true, executor);
    }

    @Override
    public RsCommandCompletionProvider getCompletionProvider(Project project, DataContext dataContext) {
        return new WasmPackCommandCompletionProvider(CargoProjectServiceUtil.getCargoProjects(project), () ->
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
        return RsIcons.WASM_PACK;
    }

    @Override
    public String getCompletionGroupTitle() {
        return RsBundle.message("wasm.pack.commands");
    }

    @Override
    public String getHelpGroupTitle() {
        return RsBundle.message("wasm.pack");
    }

    @Override
    public String getHelpCommandPlaceholder() {
        return "wasm-pack <subcommand> <args...>";
    }

    @Override
    public String getHelpCommand() {
        return HELP_COMMAND;
    }

    @Override
    public Icon getHelpIcon() {
        return RsIcons.WASM_PACK;
    }

    @Override
    public String getHelpDescription() {
        return RsBundle.message("runs.wasm.pack.command");
    }
}
