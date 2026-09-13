/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.wasmpack;


import org.rust.cargo.toolchain.RsToolchainLocator;
import consulo.execution.executor.Executor;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.configuration.ui.SettingsEditor;
import consulo.project.Project;
import consulo.process.cmd.ParametersListUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.api.settings.RsProjectSettingsServiceUtil;
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

    public WasmPackCommandConfiguration(@Nonnull Project project, @Nonnull String name, @Nonnull ConfigurationFactory factory) {
        super(project, name, factory);
    }

    @Nonnull
    @Override
    public String getCommand() {
        return command;
    }

    @Override
    public void setCommand(@Nonnull String command) {
        this.command = command;
    }

    @Nonnull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new WasmPackCommandConfigurationEditor(getProject());
    }

    @Nullable
    @Override
    public RunProfileState getState(@Nonnull Executor executor, @Nonnull ExecutionEnvironment environment) {
        RsToolchainBase toolchain = RsToolchainLocator.getToolchain(environment.getProject());
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

    public void setFromCmd(@Nonnull WasmPackCommandLine cmd) {
        List<String> args = cmd.getAdditionalArguments();
        List<String> parts = new ArrayList<>();
        parts.add(cmd.getCommand());
        parts.addAll(args);
        setCommand(ParametersListUtil.join(parts));
        setWorkingDirectory(cmd.getWorkingDirectory());
        setEmulateTerminal(cmd.getEmulateTerminal());
    }
}
