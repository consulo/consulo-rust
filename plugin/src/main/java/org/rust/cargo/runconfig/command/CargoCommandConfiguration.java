/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.command;

import com.intellij.openapi.options.advanced.AdvancedSettings;
import consulo.execution.RuntimeConfigurationException;
import consulo.execution.RuntimeConfigurationWarning;
import consulo.execution.configuration.*;
import consulo.nativeDev.profiler.NativeProfilableRunProfile;
import consulo.execution.configuration.fragment.EnvironmentFragmentState;
import consulo.execution.configuration.fragment.RunConfigurationFragment;
import consulo.execution.executor.Executor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.test.sm.runner.SMRunnerConsolePropertiesProvider;
import consulo.execution.test.sm.runner.SMTRunnerConsoleProperties;
import consulo.execution.util.ProgramParametersUtil;
import consulo.process.cmd.ParametersListUtil;
import consulo.project.Project;
import consulo.util.io.FileUtil;
import consulo.util.lang.SemVer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.api.toolchain.BacktraceMode;
import org.rust.cargo.api.toolchain.RustChannel;
import org.rust.cargo.project.model.CargoProjectLocator;
import org.rust.cargo.runconfig.CargoRunState;
import org.rust.cargo.runconfig.CargoTestRunState;
import org.rust.cargo.runconfig.RsCommandConfiguration;
import org.rust.cargo.runconfig.RunConfigUtil;
import org.rust.cargo.runconfig.target.BuildTarget;
import org.rust.cargo.runconfig.test.CargoTestConsoleProperties;
import org.rust.cargo.runconfig.ui.*;
import org.rust.cargo.toolchain.CargoCommandLine;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.toolchain.RsToolchainLocator;
import org.rust.cargo.toolchain.tools.Cargo;
import org.rust.cargo.toolchain.tools.Rustup;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * This class describes a Run Configuration.
 * It is basically a bunch of values which are persisted to .xml files in the project configuration,
 * or displayed in the GUI form. It has to be mutable to satisfy various IDE's APIs.
 */
public class CargoCommandConfiguration extends RsCommandConfiguration implements SMRunnerConsolePropertiesProvider, NativeProfilableRunProfile,
    EnvironmentAware {

    public CargoCommandConfiguration(Project project, String name, ConfigurationFactory factory) {
        super(project, name, factory);
    }

    @Override
    protected Class<? extends RunConfigurationOptions> getOptionsClass() {
        return CargoCommandConfigurationOptions.class;
    }

    @Override
    public CargoCommandConfigurationOptions getOptions() {
        return (CargoCommandConfigurationOptions) super.getOptions();
    }

    @Override
    protected void convertLegacyState(LegacyRunConfigurationConverter converter) {
        super.convertLegacyState(converter);

        CargoCommandConfigurationLegacyState old = converter.readXml(CargoCommandConfigurationLegacyState.class);

        CargoCommandConfigurationOptions options = new CargoCommandConfigurationOptions();
        convertLegacyCommandState(old, options);
        options.setChannel(old.channel);
        options.setRequiredFeatures(old.requiredFeatures);
        options.setAllFeatures(old.allFeatures);
        options.setWithSudo(old.withSudo);
        options.setBuildTarget(old.buildTarget);
        options.setBacktrace(old.backtrace);
        options.setRedirectInput(old.isRedirectInput);
        options.setRedirectInputPath(old.redirectInputPath);
        converter.setOptions(options);

        EnvironmentVariablesData env = EnvironmentVariablesData.readExternal(converter.getElement());
        converter.addFragment(new EnvironmentFragmentState(env.getEnvs(), env.isPassParentEnvs()));
    }

    @Override
    public List<RunConfigurationFragment<?>> createFragments() {
        return List.of(
            new CargoCommandFragment(),
            new CargoChannelFragment(),
            new CargoBacktraceFragment(),
            new CargoRedirectInputFragment(),
            new RsEmulateTerminalFragment<CargoCommandConfiguration>(),
            new CargoRequiredFeaturesFragment(),
            new CargoAllFeaturesFragment(),
            new CargoWithSudoFragment()
        );
    }

    public RustChannel getChannel() {
        return valueOf(RustChannel.class, getOptions().getChannel(), RustChannel.DEFAULT);
    }

    public void setChannel(RustChannel channel) {
        getOptions().setChannel(channel == RustChannel.DEFAULT ? null : channel.name());
    }

    public boolean getRequiredFeatures() {
        return getOptions().isRequiredFeatures();
    }

    public void setRequiredFeatures(boolean requiredFeatures) {
        getOptions().setRequiredFeatures(requiredFeatures);
    }

    public boolean getAllFeatures() {
        return getOptions().isAllFeatures();
    }

    public void setAllFeatures(boolean allFeatures) {
        getOptions().setAllFeatures(allFeatures);
    }

    public boolean getWithSudo() {
        return getOptions().isWithSudo();
    }

    public void setWithSudo(boolean withSudo) {
        getOptions().setWithSudo(withSudo);
    }

    public BuildTarget getBuildTarget() {
        return valueOf(BuildTarget.class, getOptions().getBuildTarget(), BuildTarget.REMOTE);
    }

    public void setBuildTarget(BuildTarget buildTarget) {
        getOptions().setBuildTarget(buildTarget == BuildTarget.REMOTE ? null : buildTarget.name());
    }

    public BacktraceMode getBacktrace() {
        return valueOf(BacktraceMode.class, getOptions().getBacktrace(), BacktraceMode.SHORT);
    }

    public void setBacktrace(BacktraceMode backtrace) {
        getOptions().setBacktrace(backtrace == BacktraceMode.SHORT ? null : backtrace.name());
    }

    public EnvironmentVariablesData getEnv() {
        return EnvironmentVariablesData.create(getEnvs(), isPassParentEnvs());
    }

    public void setEnv(EnvironmentVariablesData env) {
        setEnvs(env.getEnvs());
        setPassParentEnvs(env.isPassParentEnvs());
    }

    private static <E extends Enum<E>> E valueOf(Class<E> enumClass, @Nullable String name, E defaultValue) {
        if (name == null) {
            return defaultValue;
        }
        try {
            return Enum.valueOf(enumClass, name);
        }
        catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    @Nullable
    private File getRedirectInputFile() {
        String redirectInputPath = getRedirectInputPath();
        if (!isRedirectInput()) {
            return null;
        }
        if (redirectInputPath == null || redirectInputPath.isEmpty()) {
            return null;
        }
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
        return getOptions().isRedirectInput();
    }

    public void setRedirectInput(boolean value) {
        getOptions().setRedirectInput(value);
    }

    @Nullable
    public String getRedirectInputPath() {
        return getOptions().getRedirectInputPath();
    }

    public void setRedirectInputPath(@Nullable String value) {
        getOptions().setRedirectInputPath(value);
    }

    public void setFromCmd(CargoCommandLine cmd) {
        setChannel(cmd.getChannel());
        setCommand(toRawCommand(cmd));
        setRequiredFeatures(cmd.getRequiredFeatures());
        setAllFeatures(cmd.getAllFeatures());
        setEmulateTerminal(cmd.getEmulateTerminal());
        setWithSudo(cmd.getWithSudo());
        setBacktrace(cmd.getBacktraceMode());
        setWorkingDirectory(cmd.getWorkingDirectory());
        setEnv(cmd.getEnvironmentVariables());
        setRedirectInput(cmd.getRedirectInputFrom() != null);
        setRedirectInputPath(cmd.getRedirectInputFrom() != null ? cmd.getRedirectInputFrom().getPath() : null);
    }

    public boolean canBeFrom(CargoCommandLine cmd) {
        return getCommand().equals(toRawCommand(cmd));
    }

    private String toRawCommand(CargoCommandLine cmd) {
        List<String> parts = new ArrayList<>();
        if (cmd.getToolchain() != null) {
            parts.add("+" + cmd.getToolchain());
        }
        parts.add(cmd.getCommand());
        parts.addAll(cmd.getAdditionalArguments());
        return ParametersListUtil.join(parts);
    }

    @Override
    public void checkConfiguration() throws RuntimeConfigurationException {
        if (isRedirectInput()) {
            File file = getRedirectInputFile();
            if (file == null || !file.exists()) {
                throw new RuntimeConfigurationWarning(RsBundle.message("dialog.message.input.file.doesn.t.exist"));
            }
            if (!file.isFile()) {
                throw new RuntimeConfigurationWarning(RsBundle.message("dialog.message.input.file.not.valid"));
            }
        }

        CleanConfiguration config = clean();
        if (config instanceof CleanConfiguration.Err err) {
            throw err.getError();
        }
        CleanConfiguration.Ok ok = (CleanConfiguration.Ok) config;

        if (getWithSudo() && showTestToolWindow(ok.getCmd())) {
            String message = consulo.platform.Platform.current().os().isWindows()
                ? RsBundle.message("notification.run.tests.as.root.windows")
                : RsBundle.message("notification.run.tests.as.root.unix");
            throw new RuntimeConfigurationWarning(message);
        }
    }

    @Nullable
    @Override
    public RunProfileState getState(@Nonnull Executor executor, @Nonnull ExecutionEnvironment environment) {
        CleanConfiguration.Ok config = clean().getOk();
        if (config == null) {
            return null;
        }
        if (showTestToolWindow(config.getCmd())) {
            return new CargoTestRunState(environment, this, config);
        }
        else {
            return new CargoRunState(environment, this, config);
        }
    }

    private boolean showTestToolWindow(CargoCommandLine commandLine) {
        if (!AdvancedSettings.getBoolean(CargoTestConsoleProperties.TEST_TOOL_WINDOW_SETTING_KEY,
            CargoTestConsoleProperties.TEST_TOOL_WINDOW_DEFAULT)) {
            return false;
        }
        if (!List.of("test", "bench").contains(commandLine.getCommand())) {
            return false;
        }
        if (commandLine.getAdditionalArguments().contains("--nocapture")) {
            return false;
        }
        if (Cargo.TEST_NOCAPTURE_ENABLED_KEY.asBoolean()) {
            return false;
        }
        return !RunConfigUtil.getHasRemoteTarget(this);
    }

    @Nullable
    @Override
    public SMTRunnerConsoleProperties createTestConsoleProperties(@Nonnull Executor executor) {
        CleanConfiguration.Ok config = clean().getOk();
        if (config == null) {
            return null;
        }
        if (!showTestToolWindow(config.getCmd())) {
            return null;
        }
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

        ParsedCommand parsed = ParsedCommand.parse(getCommand());
        if (parsed == null) {
            return CleanConfiguration.error(RsBundle.message("dialog.message.no.command.specified"));
        }

        CargoCommandLine cmd = new CargoCommandLine(
            parsed.command(),
            workingDirectory,
            parsed.additionalArguments(),
            getRedirectInputFile(),
            getEmulateTerminal(),
            getBacktrace(),
            parsed.toolchain(),
            getChannel(),
            getEnv(),
            getRequiredFeatures(),
            getAllFeatures(),
            getWithSudo()
        );

        RsToolchainBase toolchain = RsToolchainLocator.getToolchain(getProject());
        if (toolchain == null) {
            return CleanConfiguration.error(RsBundle.message("dialog.message.no.rust.toolchain.specified"));
        }

        if (!toolchain.looksLikeValidToolchain()) {
            return CleanConfiguration.error(RsBundle.message("dialog.message.invalid.toolchain", toolchain.getPresentableLocation()));
        }

        RustChannel channel = getChannel();
        if (!Rustup.isRustupAvailable(toolchain) && channel != RustChannel.DEFAULT) {
            return CleanConfiguration.error(RsBundle.message("dialog.message.channel.set.explicitly.with.no.rustup.available", channel));
        }

        return new CleanConfiguration.Ok(cmd, toolchain);
    }

}
