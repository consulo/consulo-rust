/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.command;

import com.intellij.execution.Executor;
import com.intellij.execution.InputRedirectAware;
import com.intellij.execution.configuration.EnvironmentVariablesData;
import com.intellij.execution.configurations.*;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.target.LanguageRuntimeType;
import com.intellij.execution.target.TargetEnvironmentAwareRunProfile;
import com.intellij.execution.target.TargetEnvironmentConfiguration;
import com.intellij.execution.testframework.actions.ConsolePropertiesProvider;
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties;
import com.intellij.execution.util.ProgramParametersUtil;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.options.advanced.AdvancedSettings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.execution.ParametersListUtil;
import com.intellij.util.text.SemVer;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.model.CargoProjectsService;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.cargo.runconfig.*;
import org.rust.cargo.runconfig.target.BuildTarget;
import org.rust.cargo.runconfig.target.RsLanguageRuntimeConfiguration;
import org.rust.cargo.runconfig.target.RsLanguageRuntimeType;
import org.rust.cargo.runconfig.test.CargoTestConsoleProperties;
import org.rust.cargo.runconfig.ui.CargoCommandConfigurationEditor;
import org.rust.cargo.toolchain.*;
import org.rust.cargo.toolchain.tools.Cargo;
import org.rust.cargo.toolchain.tools.Rustup;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This class describes a Run Configuration.
 * It is basically a bunch of values which are persisted to .xml files inside .idea,
 * or displayed in the GUI form. It has to be mutable to satisfy various IDE's APIs.
 */
public class CargoCommandConfiguration extends RsCommandConfiguration
    implements InputRedirectAware.InputRedirectOptions, ConsolePropertiesProvider, TargetEnvironmentAwareRunProfile {

    private String command = "run";
    private RustChannel channel = RustChannel.DEFAULT;
    private boolean requiredFeatures = true;
    private boolean allFeatures = false;
    private boolean withSudo = false;
    private BuildTarget buildTarget = BuildTarget.REMOTE;
    private BacktraceMode backtrace = BacktraceMode.SHORT;
    private EnvironmentVariablesData env = EnvironmentVariablesData.DEFAULT;
    private boolean isRedirectInput = false;
    @Nullable
    private String redirectInputPath = null;

    public CargoCommandConfiguration(Project project, String name, ConfigurationFactory factory) {
        super(project, name, factory);
    }

    @Override
    public String getCommand() {
        return command;
    }

    @Override
    public void setCommand(String command) {
        this.command = command;
    }

    public RustChannel getChannel() {
        return channel;
    }

    public void setChannel(RustChannel channel) {
        this.channel = channel;
    }

    public boolean getRequiredFeatures() {
        return requiredFeatures;
    }

    public void setRequiredFeatures(boolean requiredFeatures) {
        this.requiredFeatures = requiredFeatures;
    }

    public boolean getAllFeatures() {
        return allFeatures;
    }

    public void setAllFeatures(boolean allFeatures) {
        this.allFeatures = allFeatures;
    }

    public boolean getWithSudo() {
        return withSudo;
    }

    public void setWithSudo(boolean withSudo) {
        this.withSudo = withSudo;
    }

    public BuildTarget getBuildTarget() {
        return buildTarget;
    }

    public void setBuildTarget(BuildTarget buildTarget) {
        this.buildTarget = buildTarget;
    }

    public BacktraceMode getBacktrace() {
        return backtrace;
    }

    public void setBacktrace(BacktraceMode backtrace) {
        this.backtrace = backtrace;
    }

    public EnvironmentVariablesData getEnv() {
        return env;
    }

    public void setEnv(EnvironmentVariablesData env) {
        this.env = env;
    }

    @Nullable
    private File getRedirectInputFile() {
        if (!isRedirectInput) return null;
        if (redirectInputPath == null || redirectInputPath.isEmpty()) return null;
        String path = FileUtil.toSystemDependentName(
            ProgramParametersUtil.expandPathAndMacros(redirectInputPath, null, getProject())
        );
        File file = new File(path);
        if (!file.isAbsolute() && getWorkingDirectory() != null) {
            file = new File(new File(getWorkingDirectory().toString()), path);
        }
        return file;
    }

    @Override
    public boolean isRedirectInput() {
        return isRedirectInput;
    }

    @Override
    public void setRedirectInput(boolean value) {
        isRedirectInput = value;
    }

    @Nullable
    @Override
    public String getRedirectInputPath() {
        return redirectInputPath;
    }

    @Override
    public void setRedirectInputPath(@Nullable String value) {
        redirectInputPath = value;
    }

    @Override
    public boolean canRunOn(@NotNull TargetEnvironmentConfiguration target) {
        return target.getRuntimes().findByType(RsLanguageRuntimeConfiguration.class) != null;
    }

    @Nullable
    @Override
    public LanguageRuntimeType<?> getDefaultLanguageRuntimeType() {
        return LanguageRuntimeType.EXTENSION_NAME.findExtension(RsLanguageRuntimeType.class);
    }

    @Nullable
    @Override
    public String getDefaultTargetName() {
        return getOptions().getRemoteTarget();
    }

    @Override
    public void setDefaultTargetName(@Nullable String targetName) {
        getOptions().setRemoteTarget(targetName);
    }

    @Override
    public void writeExternal(@NotNull Element element) {
        super.writeExternal(element);
        RunConfigUtil.writeEnum(element, "channel", channel);
        RunConfigUtil.writeBool(element, "requiredFeatures", requiredFeatures);
        RunConfigUtil.writeBool(element, "allFeatures", allFeatures);
        RunConfigUtil.writeBool(element, "withSudo", withSudo);
        RunConfigUtil.writeEnum(element, "buildTarget", buildTarget);
        RunConfigUtil.writeEnum(element, "backtrace", backtrace);
        env.writeExternal(element);
        RunConfigUtil.writeBool(element, "isRedirectInput", isRedirectInput);
        RunConfigUtil.writeString(element, "redirectInputPath", redirectInputPath != null ? redirectInputPath : "");
    }

    @Override
    public void readExternal(@NotNull Element element) {
        super.readExternal(element);
        String channelStr = RunConfigUtil.readString(element, "channel");
        if (channelStr != null) {
            try { channel = RustChannel.valueOf(channelStr); } catch (IllegalArgumentException ignored) {}
        }
        Boolean rf = RunConfigUtil.readBool(element, "requiredFeatures");
        if (rf != null) requiredFeatures = rf;
        Boolean af = RunConfigUtil.readBool(element, "allFeatures");
        if (af != null) allFeatures = af;
        Boolean ws = RunConfigUtil.readBool(element, "withSudo");
        if (ws != null) withSudo = ws;
        String btStr = RunConfigUtil.readString(element, "buildTarget");
        if (btStr != null) {
            try { buildTarget = BuildTarget.valueOf(btStr); } catch (IllegalArgumentException ignored) {}
        }
        String btModeStr = RunConfigUtil.readString(element, "backtrace");
        if (btModeStr != null) {
            try { backtrace = BacktraceMode.valueOf(btModeStr); } catch (IllegalArgumentException ignored) {}
        }
        env = EnvironmentVariablesData.readExternal(element);
        Boolean ri = RunConfigUtil.readBool(element, "isRedirectInput");
        if (ri != null) isRedirectInput = ri;
        String rip = RunConfigUtil.readString(element, "redirectInputPath");
        if (rip != null) redirectInputPath = rip;
    }

    public void setFromCmd(CargoCommandLine cmd) {
        channel = cmd.getChannel();
        command = toRawCommand(cmd);
        requiredFeatures = cmd.getRequiredFeatures();
        allFeatures = cmd.getAllFeatures();
        setEmulateTerminal(cmd.getEmulateTerminal());
        withSudo = cmd.getWithSudo();
        backtrace = cmd.getBacktraceMode();
        setWorkingDirectory(cmd.getWorkingDirectory());
        env = cmd.getEnvironmentVariables();
        isRedirectInput = cmd.getRedirectInputFrom() != null;
        redirectInputPath = cmd.getRedirectInputFrom() != null ? cmd.getRedirectInputFrom().getPath() : null;
    }

    public boolean canBeFrom(CargoCommandLine cmd) {
        return command.equals(toRawCommand(cmd));
    }

    private String toRawCommand(CargoCommandLine cmd) {
        List<String> parts = new ArrayList<>();
        if (cmd.getToolchain() != null) parts.add("+" + cmd.getToolchain());
        parts.add(cmd.getCommand());
        parts.addAll(cmd.getAdditionalArguments());
        return ParametersListUtil.join(parts);
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        if (isRedirectInput) {
            File file = getRedirectInputFile();
            if (file == null || !file.exists()) {
                throw new RuntimeConfigurationWarning(RsBundle.message("dialog.message.input.file.doesn.t.exist"));
            }
            if (!file.isFile()) {
                throw new RuntimeConfigurationWarning(RsBundle.message("dialog.message.input.file.not.valid"));
            }
        }

        CleanConfiguration config = clean();
        if (config instanceof CleanConfiguration.Err err) throw err.getError();
        CleanConfiguration.Ok ok = (CleanConfiguration.Ok) config;

        if (withSudo && showTestToolWindow(ok.getCmd())) {
            String message = SystemInfo.isWindows
                ? RsBundle.message("notification.run.tests.as.root.windows")
                : RsBundle.message("notification.run.tests.as.root.unix");
            throw new RuntimeConfigurationWarning(message);
        }
    }

    @NotNull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new CargoCommandConfigurationEditor(getProject());
    }

    @Nullable
    @Override
    public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {
        CleanConfiguration.Ok config = clean().getOk();
        if (config == null) return null;
        if (showTestToolWindow(config.getCmd())) {
            return new CargoTestRunState(environment, this, config);
        } else {
            return new CargoRunState(environment, this, config);
        }
    }

    private boolean showTestToolWindow(CargoCommandLine commandLine) {
        if (!AdvancedSettings.getBoolean(CargoTestConsoleProperties.TEST_TOOL_WINDOW_SETTING_KEY)) return false;
        if (!List.of("test", "bench").contains(commandLine.getCommand())) return false;
        if (commandLine.getAdditionalArguments().contains("--nocapture")) return false;
        if (Cargo.TEST_NOCAPTURE_ENABLED_KEY.asBoolean()) return false;
        return !RunConfigUtil.getHasRemoteTarget(this);
    }

    @Nullable
    @Override
    public SMTRunnerConsoleProperties createTestConsoleProperties(@NotNull Executor executor) {
        CleanConfiguration.Ok config = clean().getOk();
        if (config == null) return null;
        if (!showTestToolWindow(config.getCmd())) return null;
        CargoProject cargoProject = findCargoProject(getProject(), config.getCmd().getAdditionalArguments(), config.getCmd().getWorkingDirectory());
        SemVer version = cargoProject != null && cargoProject.getRustcInfo() != null && cargoProject.getRustcInfo().getVersion() != null
            ? cargoProject.getRustcInfo().getVersion().getSemver() : null;
        return new CargoTestConsoleProperties(this, executor, version);
    }

    // Sealed class hierarchy as static inner classes
    public static abstract class CleanConfiguration {
        @Nullable
        public Ok getOk() {
            return this instanceof Ok ok ? ok : null;
        }

        public static Err error(String message) {
            return new Err(new RuntimeConfigurationError(message));
        }

        public static final class Ok extends CleanConfiguration {
            private final CargoCommandLine cmd;
            private final RsToolchainBase toolchain;

            public Ok(CargoCommandLine cmd, RsToolchainBase toolchain) {
                this.cmd = cmd;
                this.toolchain = toolchain;
            }

            public CargoCommandLine getCmd() {
                return cmd;
            }

            public RsToolchainBase getToolchain() {
                return toolchain;
            }
        }

        public static final class Err extends CleanConfiguration {
            private final RuntimeConfigurationError error;

            public Err(RuntimeConfigurationError error) {
                this.error = error;
            }

            public RuntimeConfigurationError getError() {
                return error;
            }
        }
    }

    public CleanConfiguration clean() {
        Path workingDirectory = getWorkingDirectory();
        if (workingDirectory == null) {
            return CleanConfiguration.error(RsBundle.message("dialog.message.no.working.directory.specified"));
        }

        ParsedCommand parsed = ParsedCommand.parse(command);
        if (parsed == null) {
            return CleanConfiguration.error(RsBundle.message("dialog.message.no.command.specified"));
        }

        CargoCommandLine cmd = new CargoCommandLine(
            parsed.command(),
            workingDirectory,
            parsed.additionalArguments(),
            getRedirectInputFile(),
            getEmulateTerminal(),
            backtrace,
            parsed.toolchain(),
            channel,
            env,
            requiredFeatures,
            allFeatures,
            withSudo
        );

        RsToolchainBase toolchain = RsProjectSettingsServiceUtil.getToolchain(getProject());
        if (toolchain == null) {
            return CleanConfiguration.error(RsBundle.message("dialog.message.no.rust.toolchain.specified"));
        }

        if (!toolchain.looksLikeValidToolchain()) {
            return CleanConfiguration.error(RsBundle.message("dialog.message.invalid.toolchain", toolchain.getPresentableLocation()));
        }

        if (!Rustup.isRustupAvailable(toolchain) && channel != RustChannel.DEFAULT) {
            return CleanConfiguration.error(RsBundle.message("dialog.message.channel.set.explicitly.with.no.rustup.available", channel));
        }

        return new CleanConfiguration.Ok(cmd, toolchain);
    }

    @Nullable
    public static CargoProject findCargoProject(Project project, List<String> additionalArgs, @Nullable Path workingDirectory) {
        CargoProjectsService cargoProjects = CargoProjectServiceUtil.getCargoProjects(project);
        Collection<CargoProject> allProjects = cargoProjects.getAllProjects();
        if (allProjects.size() == 1) return allProjects.iterator().next();

        int idx = additionalArgs.indexOf("--manifest-path");
        Path manifestPath = null;
        if (idx != -1 && idx + 1 < additionalArgs.size()) {
            manifestPath = Paths.get(additionalArgs.get(idx + 1));
        }

        List<Path> dirs = new ArrayList<>();
        if (manifestPath != null && manifestPath.getParent() != null) dirs.add(manifestPath.getParent());
        if (workingDirectory != null) dirs.add(workingDirectory);

        for (Path dir : dirs) {
            VirtualFile vFile = LocalFileSystem.getInstance().findFileByIoFile(dir.toFile());
            if (vFile != null) {
                CargoProject found = cargoProjects.findProjectForFile(vFile);
                if (found != null) return found;
            }
        }
        return null;
    }

    @Nullable
    public static CargoProject findCargoProject(Project project, String cmd, @Nullable Path workingDirectory) {
        return findCargoProject(project, ParametersListUtil.parse(cmd), workingDirectory);
    }

    @Nullable
    public static CargoWorkspace.Package findCargoPackage(
        CargoProject cargoProject,
        List<String> additionalArgs,
        @Nullable Path workingDirectory
    ) {
        CargoWorkspace workspace = cargoProject.getWorkspace();
        if (workspace == null) return null;
        List<CargoWorkspace.Package> packages = new ArrayList<>();
        for (var pkg : workspace.getPackages()) {
            if (pkg.getOrigin() == PackageOrigin.WORKSPACE) {
                packages.add(pkg);
            }
        }
        if (packages.isEmpty()) return null;
        if (packages.size() == 1) return packages.get(0);

        int idx = additionalArgs.indexOf("--package");
        if (idx != -1 && idx + 1 < additionalArgs.size()) {
            String packageName = additionalArgs.get(idx + 1);
            for (var pkg : packages) {
                if (pkg.getName().equals(packageName)) return pkg;
            }
        }

        for (var pkg : packages) {
            if (pkg.getRootDirectory() != null && pkg.getRootDirectory().equals(workingDirectory)) return pkg;
        }
        return null;
    }

    public static List<CargoWorkspace.Target> findCargoTargets(
        CargoWorkspace.Package cargoPackage,
        List<String> additionalArgs
    ) {
        List<CargoWorkspace.Target> result = new ArrayList<>();
        for (CargoWorkspace.Target target : cargoPackage.getTargets()) {
            CargoWorkspace.TargetKind kind = target.getKind();
            boolean matches = false;
            if (kind == CargoWorkspace.TargetKind.Bin.INSTANCE) {
                matches = hasTarget(additionalArgs, "--bin", target.getName());
            } else if (kind == CargoWorkspace.TargetKind.Test.INSTANCE) {
                matches = hasTarget(additionalArgs, "--test", target.getName());
            } else if (kind == CargoWorkspace.TargetKind.ExampleBin.INSTANCE) {
                matches = hasTarget(additionalArgs, "--example", target.getName());
            } else if (kind == CargoWorkspace.TargetKind.Bench.INSTANCE) {
                matches = hasTarget(additionalArgs, "--bench", target.getName());
            } else if (kind.isLib()) {
                matches = additionalArgs.contains("--lib");
            } else if (kind instanceof CargoWorkspace.TargetKind.ExampleLib) {
                matches = hasTarget(additionalArgs, "--example", target.getName());
            }
            if (matches) result.add(target);
        }
        return result;
    }

    private static boolean hasTarget(List<String> args, String option, String name) {
        if (args.contains(option + "=" + name)) return true;
        for (int i = 0; i < args.size() - 1; i++) {
            if (args.get(i).equals(option) && args.get(i + 1).equals(name)) return true;
        }
        return false;
    }

    /**
     * Returns the working directory for a cargo project.
     * This is the parent directory of the project's manifest file (Cargo.toml).
     */
    @NotNull
    public static Path getWorkingDirectory(@NotNull CargoProject cargoProject) {
        return cargoProject.getManifest().getParent();
    }
}
