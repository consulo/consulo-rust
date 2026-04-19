/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain;

import com.intellij.execution.configuration.EnvironmentVariablesData;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.PtyCommandLine;
import com.intellij.execution.wsl.WslPath;
import com.intellij.util.net.HttpConfigurable;
import com.intellij.util.text.SemVer;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.CargoConstants;
import org.rust.cargo.toolchain.flavors.RsToolchainFlavor;
import org.rust.cargo.toolchain.tools.Cargo;
import org.rust.cargo.toolchain.wsl.RsWslToolchainFlavor;
import org.rust.cargo.util.ToolchainUtil;
import org.rust.openapiext.CommandLineExt;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public abstract class RsToolchainBase {

    private final Path location;

    public RsToolchainBase(Path location) {
        this.location = location;
    }

    public Path getLocation() {
        return location;
    }

    public String getPresentableLocation() {
        return pathToExecutable(Cargo.NAME).toString();
    }

    public abstract String getFileSeparator();

    public abstract int getExecutionTimeoutInMilliseconds();

    public boolean looksLikeValidToolchain() {
        return RsToolchainFlavor.getFlavor(location) != null;
    }

    /**
     * Patches passed command line to make it runnable in remote context.
     */
    public abstract GeneralCommandLine patchCommandLine(GeneralCommandLine commandLine, boolean withSudo);

    public abstract String toLocalPath(String remotePath);

    public abstract String toRemotePath(String localPath);

    public abstract String expandUserHome(String remotePath);

    public abstract String getExecutableName(String toolName);

    // for executables from toolchain
    public abstract Path pathToExecutable(String toolName);

    // for executables installed using `cargo install`
    public Path pathToCargoExecutable(String toolName) {
        // Binaries installed by `cargo install` (e.g. Grcov, Evcxr) are placed in ~/.cargo/bin by default:
        // https://doc.rust-lang.org/cargo/commands/cargo-install.html
        // But toolchain root may be different (e.g. on Arch Linux it is usually /usr/bin)
        Path exePath = pathToExecutable(toolName);
        if (Files.exists(exePath)) return exePath;
        String cargoBin = expandUserHome("~/.cargo/bin");
        String exeName = getExecutableName(toolName);
        return Paths.get(cargoBin, exeName);
    }

    public abstract boolean hasExecutable(String exec);

    public abstract boolean hasCargoExecutable(String exec);

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof RsToolchainBase)) return false;
        return location.equals(((RsToolchainBase) other).location);
    }

    @Override
    public int hashCode() {
        return location.hashCode();
    }

    public GeneralCommandLine createGeneralCommandLine(
        Path executable,
        Path workingDirectory,
        @Nullable File redirectInputFrom,
        BacktraceMode backtraceMode,
        EnvironmentVariablesData environmentVariables,
        List<String> parameters,
        boolean emulateTerminal,
        boolean withSudo,
        boolean patchToRemote,
        HttpConfigurable http
    ) {
        GeneralCommandLine commandLine = CommandLineExt.newCommandLine(executable, withSudo);
        CommandLineExt.withWorkDirectory(commandLine, workingDirectory);
        commandLine.withInput(redirectInputFrom);
        commandLine.withEnvironment("TERM", "ansi");
        commandLine.withParameters(parameters);
        commandLine.withCharset(java.nio.charset.StandardCharsets.UTF_8);
        commandLine.withRedirectErrorStream(true);
        ProxyHelper.withProxyIfNeeded(commandLine, http);

        switch (backtraceMode) {
            case SHORT:
                commandLine.withEnvironment(CargoConstants.RUST_BACKTRACE_ENV_VAR, "short");
                break;
            case FULL:
                commandLine.withEnvironment(CargoConstants.RUST_BACKTRACE_ENV_VAR, "full");
                break;
            case NO:
                break;
        }

        environmentVariables.configureCommandLine(commandLine, true);

        if (emulateTerminal) {
            commandLine = new PtyCommandLine(commandLine)
                .withInitialColumns(PtyCommandLine.MAX_COLUMNS)
                .withConsoleMode(false);
        }

        if (patchToRemote) {
            commandLine = patchCommandLine(commandLine, withSudo);
        }

        return commandLine;
    }

    public GeneralCommandLine createGeneralCommandLine(
        Path executable,
        Path workingDirectory,
        @Nullable File redirectInputFrom,
        BacktraceMode backtraceMode,
        EnvironmentVariablesData environmentVariables,
        List<String> parameters,
        boolean emulateTerminal,
        boolean withSudo
    ) {
        return createGeneralCommandLine(
            executable, workingDirectory, redirectInputFrom, backtraceMode,
            environmentVariables, parameters, emulateTerminal, withSudo,
            true, HttpConfigurable.getInstance()
        );
    }

    public static final SemVer MIN_SUPPORTED_TOOLCHAIN = ToolchainUtil.parseSemVer("1.56.0");

    /**
     * Environment variable to unlock unstable features of rustc and cargo.
     * It doesn't change real toolchain.
     *
     * @see <a href="https://github.com/rust-lang/cargo/blob/06ddf3557796038fd87743bd3b6530676e12e719/src/cargo/core/features.rs#L447">features.rs</a>
     */
    public static final String RUSTC_BOOTSTRAP = "RUSTC_BOOTSTRAP";
    public static final String RUSTC_WRAPPER = "RUSTC_WRAPPER";

    /**
     * Environment variable used to keep original value of RUSTC_BOOTSTRAP variable
     * to be able to restore original value for subprocesses if needed
     */
    public static final String ORIGINAL_RUSTC_BOOTSTRAP = "INTELLIJ_ORIGINAL_RUSTC_BOOTSTRAP";

    @Nullable
    public static RsToolchainBase suggest(@Nullable Path projectDir) {
        if (projectDir != null) {
            com.intellij.execution.wsl.WSLDistribution distribution =
                WslPath.getDistributionByWindowsUncPath(projectDir.toString());
            if (distribution != null) {
                for (Path candidate : RsWslToolchainFlavor.getDistributionHomePathCandidates(distribution)) {
                    if (RsToolchainFlavor.getFlavor(candidate) != null) {
                        RsToolchainBase toolchain = RsToolchainProvider.getToolchainStatic(candidate.toAbsolutePath());
                        if (toolchain != null) return toolchain;
                    }
                }
            }
        }

        for (RsToolchainFlavor flavor : RsToolchainFlavor.getApplicableFlavors()) {
            for (Path homePath : (Iterable<Path>) flavor.suggestHomePaths()::iterator) {
                RsToolchainBase toolchain = RsToolchainProvider.getToolchainStatic(homePath.toAbsolutePath());
                if (toolchain != null) return toolchain;
            }
        }
        return null;
    }

    @Nullable
    public static RsToolchainBase suggest() {
        return suggest(null);
    }
}
