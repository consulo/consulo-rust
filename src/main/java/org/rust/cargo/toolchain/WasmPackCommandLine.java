/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain;

import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.runconfig.RsCommandConfiguration;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import org.rust.cargo.runconfig.wasmpack.WasmPackCommandConfiguration;
import org.rust.cargo.runconfig.wasmpack.WasmPackCommandConfigurationType;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class WasmPackCommandLine extends RsCommandLineBase {

    private final String command;
    private final Path workingDirectory;
    private final List<String> additionalArguments;
    private final boolean emulateTerminal;

    public WasmPackCommandLine(
        String command,
        Path workingDirectory,
        List<String> additionalArguments,
        boolean emulateTerminal
    ) {
        this.command = command;
        this.workingDirectory = workingDirectory;
        this.additionalArguments = additionalArguments;
        this.emulateTerminal = emulateTerminal;
    }

    public WasmPackCommandLine(String command, Path workingDirectory) {
        this(command, workingDirectory, Collections.emptyList(),
            RsCommandConfiguration.getEmulateTerminalDefault());
    }

    @Override
    public String getCommand() {
        return command;
    }

    @Override
    public Path getWorkingDirectory() {
        return workingDirectory;
    }

    @Override
    public List<String> getAdditionalArguments() {
        return additionalArguments;
    }

    @Override
    @Nullable
    public File getRedirectInputFrom() {
        return null;
    }

    @Override
    public boolean getEmulateTerminal() {
        return emulateTerminal;
    }

    @Override
    protected String getExecutableName() {
        return "wasm-pack";
    }

    @Override
    protected RunnerAndConfigurationSettings createRunConfiguration(RunManagerEx runManager, @Nullable String name) {
        RunnerAndConfigurationSettings runnerAndConfigurationSettings = runManager.createConfiguration(
            name != null ? name : command,
            ConfigurationTypeUtil.findConfigurationType(WasmPackCommandConfigurationType.class).getFactory()
        );
        WasmPackCommandConfiguration configuration =
            (WasmPackCommandConfiguration) runnerAndConfigurationSettings.getConfiguration();
        configuration.setFromCmd(this);
        return runnerAndConfigurationSettings;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WasmPackCommandLine)) return false;
        WasmPackCommandLine that = (WasmPackCommandLine) o;
        return emulateTerminal == that.emulateTerminal &&
            Objects.equals(command, that.command) &&
            Objects.equals(workingDirectory, that.workingDirectory) &&
            Objects.equals(additionalArguments, that.additionalArguments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(command, workingDirectory, additionalArguments, emulateTerminal);
    }

    @Override
    public String toString() {
        return "WasmPackCommandLine(" +
            "command=" + command +
            ", workingDirectory=" + workingDirectory +
            ", additionalArguments=" + additionalArguments +
            ")";
    }
}
