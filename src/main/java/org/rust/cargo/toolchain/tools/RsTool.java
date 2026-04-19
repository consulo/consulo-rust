/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain.tools;

import com.intellij.execution.configurations.GeneralCommandLine;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.openapiext.CommandLineExt;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class RsTool {

    private final Path executable;
    private final RsToolchainBase toolchain;

    public RsTool(String toolName, RsToolchainBase toolchain) {
        this.toolchain = toolchain;
        this.executable = toolchain.pathToExecutable(toolName);
    }

    protected RsTool(String toolName, RsToolchainBase toolchain, boolean useCargoBinary) {
        this.toolchain = toolchain;
        this.executable = useCargoBinary
            ? toolchain.pathToCargoExecutable(toolName)
            : toolchain.pathToExecutable(toolName);
    }

    public Path getExecutable() {
        return executable;
    }

    public RsToolchainBase getToolchain() {
        return toolchain;
    }

    protected GeneralCommandLine createBaseCommandLine(String... parameters) {
        return createBaseCommandLine(Arrays.asList(parameters), null, Collections.emptyMap());
    }

    protected GeneralCommandLine createBaseCommandLine(
        String[] parameters,
        Path workingDirectory,
        Map<String, String> environment
    ) {
        return createBaseCommandLine(Arrays.asList(parameters), workingDirectory, environment);
    }

    protected GeneralCommandLine createBaseCommandLine(
        List<String> parameters,
        Path workingDirectory,
        Map<String, String> environment
    ) {
        GeneralCommandLine cmd = CommandLineExt.newCommandLine(executable, false);
        CommandLineExt.withWorkDirectory(cmd, workingDirectory);
        cmd.withParameters(parameters);
        cmd.withEnvironment(environment);
        cmd.withCharset(java.nio.charset.StandardCharsets.UTF_8);
        toolchain.patchCommandLine(cmd, false);
        return cmd;
    }

    protected GeneralCommandLine createBaseCommandLine(
        List<String> parameters,
        Path workingDirectory
    ) {
        return createBaseCommandLine(parameters, workingDirectory, Collections.emptyMap());
    }
}
