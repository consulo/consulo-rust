/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.command;

import org.rust.cargo.api.toolchain.BacktraceMode;
import org.rust.cargo.api.toolchain.RustChannel;
import org.rust.cargo.toolchain.RsToolchainLocator;
import consulo.execution.RuntimeConfigurationWarning;

import consulo.execution.executor.Executor;
import com.intellij.execution.InputRedirectAware;
import consulo.execution.configuration.EnvironmentVariablesData;
import consulo.execution.configuration.LocatableConfigurationBase;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.configuration.RuntimeConfigurationError;
import consulo.execution.RuntimeConfigurationException;
import com.intellij.execution.configurations.RunConfigurationWithSuppressedDefaultDebugAction;
import com.intellij.execution.configurations.PtyCommandLine;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.cmd.ParametersList;
import consulo.execution.runner.ExecutionEnvironment;
import com.intellij.execution.target.LanguageRuntimeType;
import com.intellij.execution.target.TargetEnvironmentAwareRunProfile;
import com.intellij.execution.target.TargetEnvironmentConfiguration;
import com.intellij.execution.testframework.actions.ConsolePropertiesProvider;
import consulo.execution.test.sm.runner.SMTRunnerConsoleProperties;
import consulo.execution.util.ProgramParametersUtil;
import consulo.execution.configuration.ui.SettingsEditor;
import com.intellij.openapi.options.advanced.AdvancedSettings;
import consulo.project.Project;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.process.cmd.ParametersListUtil;
import consulo.util.lang.SemVer;
import org.jdom.Element;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectLocator;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.api.model.CargoProjectsService;
import org.rust.cargo.api.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.api.workspace.CargoWorkspace;
import org.rust.cargo.api.workspace.PackageOrigin;
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
import consulo.platform.Platform;

/**
 * This class describes a Run Configuration.
 * It is basically a bunch of values which are persisted to .xml files in the project configuration,
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
            ProgramParametersUtil.expandPath(redirectInputPath, null, getProject())
        );
        File file = new File(path);
        if (!file.isAbsolute() && getWorkingDirectory() != null) {
            file = new File(new File(getWorkingDirectory().toString()), path);
        }
        return file;
    }

    public boolean isRedirectInput() {
        return isRedirectInput;
    }

    public void setRedirectInput(boolean value) {
        isRedirectInput = value;
    }

    @Nullable
    public String getRedirectInputPath() {
        return redirectInputPath;
    }

    public void setRedirectInputPath(@Nullable String value) {
        redirectInputPath = value;
    }

    public boolean canRunOn(@Nonnull TargetEnvironmentConfiguration target) {
        return target.getRuntimes().findByType(RsLanguageRuntimeConfiguration.class) != null;
    }

    @Nullable
    public LanguageRuntimeType<?> getDefaultLanguageRuntimeType() {
        return LanguageRuntimeType.EXTENSION_NAME.findExtension(RsLanguageRuntimeType.class);
    }

    @Nullable
    public String getDefaultTargetName() {
        return null;
    }

    public void setDefaultTargetName(@Nullable String targetName) {
        // no-op: remote target storage not implemented in Consulo port yet
    }

    @Override
    public void writeExternal(@Nonnull Element element) {
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
    public void readExternal(@Nonnull Element element) {
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
            String message = consulo.platform.Platform.current().os().isWindows()
                ? RsBundle.message("notification.run.tests.as.root.windows")
                : RsBundle.message("notification.run.tests.as.root.unix");
            throw new RuntimeConfigurationWarning(message);
        }
    }

    @Nonnull
    @Override
    public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
        return new CargoCommandConfigurationEditor(getProject());
    }

    @Nullable
    @Override
    public RunProfileState getState(@Nonnull Executor executor, @Nonnull ExecutionEnvironment environment) {
        CleanConfiguration.Ok config = clean().getOk();
        if (config == null) return null;
        if (showTestToolWindow(config.getCmd())) {
            return new CargoTestRunState(environment, this, config);
        } else {
            return new CargoRunState(environment, this, config);
        }
    }

    private boolean showTestToolWindow(CargoCommandLine commandLine) {
        if (!AdvancedSettings.getBoolean(CargoTestConsoleProperties.TEST_TOOL_WINDOW_SETTING_KEY,
            CargoTestConsoleProperties.TEST_TOOL_WINDOW_DEFAULT)) return false;
        if (!List.of("test", "bench").contains(commandLine.getCommand())) return false;
        if (commandLine.getAdditionalArguments().contains("--nocapture")) return false;
        if (Cargo.TEST_NOCAPTURE_ENABLED_KEY.asBoolean()) return false;
        return !RunConfigUtil.getHasRemoteTarget(this);
    }

    @Nullable
    @Override
    public SMTRunnerConsoleProperties createTestConsoleProperties(@Nonnull Executor executor) {
        CleanConfiguration.Ok config = clean().getOk();
        if (config == null) return null;
        if (!showTestToolWindow(config.getCmd())) return null;
        CargoProject cargoProject = CargoProjectLocator.findCargoProject(getProject(), config.getCmd().getAdditionalArguments(), config.getCmd().getWorkingDirectory());
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

        RsToolchainBase toolchain = RsToolchainLocator.getToolchain(getProject());
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

}
