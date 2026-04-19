/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain;

import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configuration.EnvironmentVariablesData;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.runconfig.RsCommandConfiguration;
import org.rust.cargo.runconfig.RunConfigUtil;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class CargoCommandLine extends RsCommandLineBase {

    private final String command;
    private final Path workingDirectory;
    private final List<String> additionalArguments;
    @Nullable
    private final File redirectInputFrom;
    private final boolean emulateTerminal;
    private final BacktraceMode backtraceMode;
    @Nullable
    private final String toolchain;
    private final RustChannel channel;
    private final EnvironmentVariablesData environmentVariables;
    private final boolean requiredFeatures;
    private final boolean allFeatures;
    private final boolean withSudo;

    public CargoCommandLine(
        String command,
        Path workingDirectory,
        List<String> additionalArguments,
        @Nullable File redirectInputFrom,
        boolean emulateTerminal,
        BacktraceMode backtraceMode,
        @Nullable String toolchain,
        RustChannel channel,
        EnvironmentVariablesData environmentVariables,
        boolean requiredFeatures,
        boolean allFeatures,
        boolean withSudo
    ) {
        this.command = command;
        this.workingDirectory = workingDirectory;
        this.additionalArguments = additionalArguments;
        this.redirectInputFrom = redirectInputFrom;
        this.emulateTerminal = emulateTerminal;
        this.backtraceMode = backtraceMode;
        this.toolchain = toolchain;
        this.channel = channel;
        this.environmentVariables = environmentVariables;
        this.requiredFeatures = requiredFeatures;
        this.allFeatures = allFeatures;
        this.withSudo = withSudo;
    }

    public CargoCommandLine(String command, Path workingDirectory, List<String> additionalArguments) {
        this(command, workingDirectory, additionalArguments, null,
            RsCommandConfiguration.getEmulateTerminalDefault(),
            BacktraceMode.DEFAULT, null, RustChannel.DEFAULT,
            EnvironmentVariablesData.DEFAULT, true, false, false);
    }

    public CargoCommandLine(String command, Path workingDirectory) {
        this(command, workingDirectory, Collections.emptyList());
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
        return redirectInputFrom;
    }

    @Override
    public boolean getEmulateTerminal() {
        return emulateTerminal;
    }

    public BacktraceMode getBacktraceMode() {
        return backtraceMode;
    }

    @Nullable
    public String getToolchain() {
        return toolchain;
    }

    public RustChannel getChannel() {
        return channel;
    }

    public EnvironmentVariablesData getEnvironmentVariables() {
        return environmentVariables;
    }

    public boolean getRequiredFeatures() {
        return requiredFeatures;
    }

    public boolean getAllFeatures() {
        return allFeatures;
    }

    public boolean getWithSudo() {
        return withSudo;
    }

    @Override
    protected String getExecutableName() {
        return "cargo";
    }

    @Override
    protected RunnerAndConfigurationSettings createRunConfiguration(RunManagerEx runManager, @Nullable String name) {
        return RunConfigUtil.createCargoCommandRunConfiguration(runManager, this, name);
    }

    public CargoCommandLine copy(
        String command,
        Path workingDirectory,
        List<String> additionalArguments,
        @Nullable File redirectInputFrom,
        boolean emulateTerminal,
        BacktraceMode backtraceMode,
        @Nullable String toolchain,
        RustChannel channel,
        EnvironmentVariablesData environmentVariables,
        boolean requiredFeatures,
        boolean allFeatures,
        boolean withSudo
    ) {
        return new CargoCommandLine(
            command, workingDirectory, additionalArguments, redirectInputFrom,
            emulateTerminal, backtraceMode, toolchain, channel, environmentVariables,
            requiredFeatures, allFeatures, withSudo
        );
    }

    public CargoCommandLine copy(String command, List<String> additionalArguments) {
        return copy(command, workingDirectory, additionalArguments, redirectInputFrom,
            emulateTerminal, backtraceMode, toolchain, channel, environmentVariables,
            requiredFeatures, allFeatures, withSudo);
    }

    public CargoCommandLine withSudo(boolean withSudo) {
        return copy(command, workingDirectory, additionalArguments, redirectInputFrom,
            emulateTerminal, backtraceMode, toolchain, channel, environmentVariables,
            requiredFeatures, allFeatures, withSudo);
    }

    public CargoCommandLine withAdditionalArguments(List<String> additionalArguments) {
        return copy(command, workingDirectory, additionalArguments, redirectInputFrom,
            emulateTerminal, backtraceMode, toolchain, channel, environmentVariables,
            requiredFeatures, allFeatures, withSudo);
    }

    public CargoCommandLine withEmulateTerminal(boolean emulateTerminal) {
        return copy(command, workingDirectory, additionalArguments, redirectInputFrom,
            emulateTerminal, backtraceMode, toolchain, channel, environmentVariables,
            requiredFeatures, allFeatures, withSudo);
    }

    /**
     * Adds arg to additionalArguments as a positional argument, in other words, inserts arg right after
     * -- argument in additionalArguments.
     */
    public CargoCommandLine withPositionalArgument(String arg) {
        List<List<String>> split = splitOnDoubleDash();
        List<String> pre = split.get(0);
        List<String> post = split.get(1);
        if (post.contains(arg)) return this;
        List<String> newArgs = new ArrayList<>(pre);
        newArgs.add("--");
        newArgs.add(arg);
        newArgs.addAll(post);
        return withAdditionalArguments(newArgs);
    }

    /**
     * Splits additionalArguments into parts before and after --.
     * For `cargo run --release -- foo bar`, returns [["--release"], ["foo", "bar"]]
     */
    public List<List<String>> splitOnDoubleDash() {
        return org.rust.cargo.util.CargoArgsParserUtil.splitOnDoubleDash(additionalArguments);
    }

    public CargoCommandLine prependArgument(String arg) {
        List<String> newArgs = new ArrayList<>();
        newArgs.add(arg);
        newArgs.addAll(additionalArguments);
        return withAdditionalArguments(newArgs);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CargoCommandLine)) return false;
        CargoCommandLine that = (CargoCommandLine) o;
        return emulateTerminal == that.emulateTerminal &&
            requiredFeatures == that.requiredFeatures &&
            allFeatures == that.allFeatures &&
            withSudo == that.withSudo &&
            Objects.equals(command, that.command) &&
            Objects.equals(workingDirectory, that.workingDirectory) &&
            Objects.equals(additionalArguments, that.additionalArguments) &&
            Objects.equals(redirectInputFrom, that.redirectInputFrom) &&
            backtraceMode == that.backtraceMode &&
            Objects.equals(toolchain, that.toolchain) &&
            channel == that.channel &&
            Objects.equals(environmentVariables, that.environmentVariables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(command, workingDirectory, additionalArguments, redirectInputFrom,
            emulateTerminal, backtraceMode, toolchain, channel, environmentVariables,
            requiredFeatures, allFeatures, withSudo);
    }

    @Override
    public String toString() {
        return "CargoCommandLine(" +
            "command=" + command +
            ", workingDirectory=" + workingDirectory +
            ", additionalArguments=" + additionalArguments +
            ", backtraceMode=" + backtraceMode +
            ", channel=" + channel +
            ")";
    }

    // Static factory methods

    public static CargoCommandLine forTargets(
        List<CargoWorkspace.Target> targets,
        String command,
        List<String> additionalArguments,
        RustChannel channel,
        EnvironmentVariablesData environmentVariables,
        boolean usePackageOption,
        boolean isDoctest
    ) {
        List<CargoWorkspace.Package> pkgs = targets.stream().map(CargoWorkspace.Target::getPkg).collect(Collectors.toList());
        // Make sure the selection does not span more than one package.
        assert pkgs.stream().map(CargoWorkspace.Package::getRootDirectory).distinct().count() == 1;
        CargoWorkspace.Package pkg = pkgs.get(0);

        List<String> targetArgs = new ArrayList<>();
        Set<String> seenNames = new HashSet<>();
        for (CargoWorkspace.Target target : targets) {
            if (!seenNames.add(target.getName())) continue;
            CargoWorkspace.TargetKind kind = target.getKind();
            if (kind == CargoWorkspace.TargetKind.Bin.INSTANCE) {
                targetArgs.add("--bin");
                targetArgs.add(target.getName());
            } else if (kind == CargoWorkspace.TargetKind.Test.INSTANCE) {
                targetArgs.add("--test");
                targetArgs.add(target.getName());
            } else if (kind == CargoWorkspace.TargetKind.ExampleBin.INSTANCE || kind instanceof CargoWorkspace.TargetKind.ExampleLib) {
                targetArgs.add("--example");
                targetArgs.add(target.getName());
            } else if (kind == CargoWorkspace.TargetKind.Bench.INSTANCE) {
                targetArgs.add("--bench");
                targetArgs.add(target.getName());
            } else if (kind instanceof CargoWorkspace.TargetKind.Lib) {
                if (isDoctest) {
                    targetArgs.add("--doc");
                } else {
                    targetArgs.add("--lib");
                }
            }
            // CustomBuild and Unknown produce no arguments
        }

        Path workingDirectory;
        if (usePackageOption) {
            workingDirectory = pkg.getWorkspace().getContentRoot();
        } else {
            workingDirectory = pkg.getRootDirectory();
        }

        List<String> commandLineArguments = new ArrayList<>();
        if (usePackageOption) {
            commandLineArguments.add("--package");
            commandLineArguments.add(pkg.getName());
        }
        commandLineArguments.addAll(targetArgs);
        commandLineArguments.addAll(additionalArguments);

        return new CargoCommandLine(
            command,
            workingDirectory,
            commandLineArguments,
            null,
            RsCommandConfiguration.getEmulateTerminalDefault(),
            BacktraceMode.DEFAULT,
            null,
            channel,
            environmentVariables,
            true,
            false,
            false
        );
    }

    public static CargoCommandLine forTargets(
        List<CargoWorkspace.Target> targets,
        String command,
        List<String> additionalArguments,
        RustChannel channel,
        EnvironmentVariablesData environmentVariables,
        boolean usePackageOption
    ) {
        return forTargets(targets, command, additionalArguments, channel, environmentVariables, usePackageOption, false);
    }

    public static CargoCommandLine forTarget(
        CargoWorkspace.Target target,
        String command,
        List<String> additionalArguments,
        RustChannel channel,
        EnvironmentVariablesData environmentVariables,
        boolean usePackageOption
    ) {
        return forTargets(List.of(target), command, additionalArguments, channel, environmentVariables, usePackageOption);
    }

    public static CargoCommandLine forTargets(
        List<CargoWorkspace.Target> targets,
        String command,
        List<String> additionalArguments,
        boolean isDoctest
    ) {
        return forTargets(targets, command, additionalArguments, RustChannel.DEFAULT, EnvironmentVariablesData.DEFAULT, true, isDoctest);
    }

    public static CargoCommandLine forTarget(
        CargoWorkspace.Target target,
        String command,
        List<String> additionalArguments
    ) {
        return forTarget(target, command, additionalArguments, RustChannel.DEFAULT, EnvironmentVariablesData.DEFAULT, true);
    }

    public static CargoCommandLine forProject(
        CargoProject cargoProject,
        String command,
        List<String> additionalArguments,
        boolean emulateTerminal,
        @Nullable String toolchain,
        RustChannel channel,
        EnvironmentVariablesData environmentVariables
    ) {
        return new CargoCommandLine(
            command,
            CargoCommandConfiguration.getWorkingDirectory(cargoProject),
            additionalArguments,
            null,
            emulateTerminal,
            BacktraceMode.DEFAULT,
            toolchain,
            channel,
            environmentVariables,
            true,
            false,
            false
        );
    }

    public static CargoCommandLine forProject(
        CargoProject cargoProject,
        String command,
        List<String> additionalArguments
    ) {
        return forProject(cargoProject, command, additionalArguments,
            RsCommandConfiguration.getEmulateTerminalDefault(),
            null, RustChannel.DEFAULT, EnvironmentVariablesData.DEFAULT);
    }

    public static CargoCommandLine forProject(CargoProject cargoProject, String command) {
        return forProject(cargoProject, command, Collections.emptyList());
    }

    public static CargoCommandLine forPackage(
        CargoWorkspace.Package cargoPackage,
        String command,
        List<String> additionalArguments
    ) {
        List<String> args = new ArrayList<>();
        args.add("--package");
        args.add(cargoPackage.getName());
        args.addAll(additionalArguments);
        return new CargoCommandLine(
            command,
            cargoPackage.getWorkspace().getManifestPath().getParent(),
            args
        );
    }

    public static CargoCommandLine forPackage(CargoWorkspace.Package cargoPackage, String command) {
        return forPackage(cargoPackage, command, Collections.emptyList());
    }
}
