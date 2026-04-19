/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.wasmpack;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.util.execution.ParametersListUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.runconfig.RsCommandConfiguration;
import org.rust.cargo.runconfig.ui.WasmPackCommandConfigurationEditor;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.toolchain.WasmPackCommandLine;
import org.rust.cargo.toolchain.tools.WasmPack;
import org.rust.stdext.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class WasmPackCommandConfiguration extends RsCommandConfiguration {

    private String command = "build";

    public WasmPackCommandConfiguration(@NotNull Project project, @NotNull String name, @NotNull ConfigurationFactory factory) {
        super(project, name, factory);
    }

    @NotNull
    @Override
    public String getCommand() {
        return command;
    }

    @Override
    public void setCommand(@NotNull String command) {
        this.command = command;
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new WasmPackCommandConfigurationEditor(getProject());
    }

    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {
        RsToolchainBase toolchain = RsProjectSettingsServiceUtil.getToolchain(environment.getProject());
        if (toolchain == null) return null;
        var wasmPack = WasmPack.create(toolchain);
        if (wasmPack == null) return null;
        File workingDir = getWorkingDirectory() != null ? getWorkingDirectory().toFile() : null;
        if (workingDir == null) return null;
        return new WasmPackCommandRunState(environment, this, wasmPack, workingDir);
    }

    @Nullable
    @Override
    public String suggestedName() {
        String cmd = getCommand();
        String firstWord = cmd.contains(" ") ? cmd.substring(0, cmd.indexOf(' ')) : cmd;
        return Utils.capitalized(firstWord);
    }

    public void setFromCmd(@NotNull WasmPackCommandLine cmd) {
        List<String> args = cmd.getAdditionalArguments();
        List<String> parts = new ArrayList<>();
        parts.add(cmd.getCommand());
        parts.addAll(args);
        setCommand(ParametersListUtil.join(parts));
        setWorkingDirectory(cmd.getWorkingDirectory());
        setEmulateTerminal(cmd.getEmulateTerminal());
    }
}
