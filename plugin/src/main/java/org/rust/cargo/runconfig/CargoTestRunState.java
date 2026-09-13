/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig;

import consulo.execution.DefaultExecutionResult;
import consulo.execution.ExecutionResult;
import consulo.execution.executor.Executor;
import consulo.execution.configuration.EnvironmentVariablesData;
import consulo.execution.ui.console.BrowserHyperlinkInfo;
import consulo.execution.ui.console.Filter;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.runner.ProgramRunner;
import consulo.execution.test.action.ToggleAutoTestAction;
import consulo.project.ui.notification.NotificationType;
import com.intellij.openapi.options.advanced.AdvancedSettings;
import consulo.project.Project;
import consulo.application.util.HtmlChunk;
import consulo.util.lang.SemVer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.runconfig.console.CargoTestConsoleBuilder;
import org.rust.cargo.runconfig.test.CargoTestConsoleProperties;
import org.rust.cargo.toolchain.CargoCommandLine;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.api.toolchain.RustChannel;
import org.rust.cargo.api.toolchain.RustcVersion;
import org.rust.cargo.api.util.ToolchainUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import consulo.application.ApplicationPropertiesComponent;
import consulo.execution.ui.console.ConsoleView;
import consulo.platform.Platform;
import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import org.rust.notifications.NotificationUtils;

public class CargoTestRunState extends CargoRunStateBase {

    // Stable Rust test framework does not support `-Z unstable-options --format json` since 1.70.0-beta
    // (https://github.com/rust-lang/rust/pull/109044)
    private static final SemVer RUSTC_1_70_BETA = ToolchainUtil.parseSemVer("1.70.0-beta");

    private static final String DO_NOT_SHOW_KEY = "org.rust.cargo.test.rustc.bootstrap.do.not.show";

    private static final String CHANGES_URL = "https://blog.rust-lang.org/2023/06/01/Rust-1.70.0.html#enforced-stability-in-the-test-cli";

    private final Function<CargoCommandLine, CargoCommandLine> myCargoTestPatch;

    public CargoTestRunState(
        @Nonnull ExecutionEnvironment environment,
        @Nonnull CargoCommandConfiguration runConfiguration,
        @Nonnull CargoCommandConfiguration.CleanConfiguration.Ok config
    ) {
        super(environment, runConfiguration, config);

        myCargoTestPatch = commandLine -> {
            RustcVersion rustcVersion = getCargoProject() != null && getCargoProject().getRustcInfo() != null
                ? getCargoProject().getRustcInfo().getVersion() : null;
            // Stable Rust test framework does not support `-Z unstable-options --format json` since 1.70.0-beta
            // (https://github.com/rust-lang/rust/pull/109044)
            boolean requiresRustcBootstrap = !(rustcVersion != null
                && (rustcVersion.getChannel() == RustChannel.NIGHTLY
                || rustcVersion.getChannel() == RustChannel.DEV
                || rustcVersion.getSemver().compareTo(RUSTC_1_70_BETA) < 0));
            EnvironmentVariablesData environmentVariables;
            if (requiresRustcBootstrap) {
                if (!ApplicationPropertiesComponent.getInstance().getBoolean(DO_NOT_SHOW_KEY, false)) {
                    showRustcBootstrapWarning(getProject());
                }
                EnvironmentVariablesData oldVariables = commandLine.getEnvironmentVariables();
                Map<String, String> newEnvs = new HashMap<>(oldVariables.getEnvs());
                newEnvs.put(RsToolchainBase.RUSTC_BOOTSTRAP, "1");
                environmentVariables = EnvironmentVariablesData.create(newEnvs, oldVariables.isPassParentEnvs());
            } else {
                environmentVariables = commandLine.getEnvironmentVariables();
            }

            // TODO: always pass `withSudo` once elevated execution supports error stream redirection
            // https://github.com/intellij-rust/intellij-rust/issues/7320
            if (commandLine.getWithSudo()) {
                String message;
                if (consulo.platform.Platform.current().os().isWindows()) {
                    message = RsBundle.message("notification.run.tests.as.root.windows");
                } else {
                    message = RsBundle.message("notification.run.tests.as.root.unix");
                }
                org.rust.notifications.NotificationUtils.showBalloon(getProject(), message, NotificationType.WARNING, null);
            }

            return commandLine.copy(
                commandLine.getCommand(),
                commandLine.getWorkingDirectory(),
                patchArgs(commandLine),
                commandLine.getRedirectInputFrom(),
                false, // emulateTerminal
                commandLine.getBacktraceMode(),
                commandLine.getToolchain(),
                commandLine.getChannel(),
                environmentVariables,
                commandLine.getRequiredFeatures(),
                commandLine.getAllFeatures(),
                false  // withSudo
            );
        };

        setConsoleBuilder(new CargoTestConsoleBuilder(
            (CargoCommandConfiguration) environment.getRunProfile(),
            environment.getExecutor()
        ));
        myCommandLinePatches.add(myCargoTestPatch);
        for (Filter filter : RunConfigUtil.createFilters(getCargoProject())) {
            getConsoleBuilder().addFilter(filter);
        }
    }

    @Nonnull
    @Override
    public ExecutionResult execute(@Nonnull Executor executor, @Nonnull ProgramRunner runner) throws consulo.process.ExecutionException {
        consulo.process.ProcessHandler processHandler = startProcess();
        consulo.execution.ui.console.ConsoleView console = createConsole(executor);
        if (console != null) {
            console.attachToProcess(processHandler);
        }
        DefaultExecutionResult result = new DefaultExecutionResult(console, processHandler);
        result.setRestartActions(new ToggleAutoTestAction());
        return result;
    }

    private static void showRustcBootstrapWarning(@Nonnull Project project) {
        String RUSTC_BOOTSSTRAP = RsToolchainBase.RUSTC_BOOTSTRAP + "=1";
        String content = RsBundle.message(
            "rustc.bootstrap.warning",
            HtmlChunk.link("changes", RsBundle.message("rust.1.70.0.stable")),
            HtmlChunk.text(RsBundle.message("cargo.test")).bold(),
            HtmlChunk.text(RUSTC_BOOTSSTRAP).bold(),
            HtmlChunk.br(),
            HtmlChunk.link("disable", RsBundle.message("disabling.the.test.tool.window"))
        );
        org.rust.notifications.NotificationUtils.showBalloon(
            project,
            RsBundle.message("notification.title.potentially.inconsistent.build.test.results"),
            content,
            NotificationType.WARNING,
            null,
            (notification, event) -> {
                if ("changes".equals(event.getDescription())) {
                    new BrowserHyperlinkInfo(CHANGES_URL).navigate(project);
                } else if ("disable".equals(event.getDescription())) {
                    notification.expire();
                    AdvancedSettings.setBoolean(CargoTestConsoleProperties.TEST_TOOL_WINDOW_SETTING_KEY, false);
                    showConfirmationInfo(project);
                }
            }
        );

        ApplicationPropertiesComponent.getInstance().setValue(DO_NOT_SHOW_KEY, true);
    }

    private static void showConfirmationInfo(@Nonnull Project project) {
        String content = RsBundle.message(
            "the.0.tool.window.was.disabled.1.2",
            HtmlChunk.text(RsBundle.message("test")).bold(),
            HtmlChunk.br(),
            HtmlChunk.link("revert", RsBundle.message("revert"))
        );
        org.rust.notifications.NotificationUtils.showBalloon(
            project,
            "",
            content,
            NotificationType.INFORMATION,
            null,
            (notification, event) -> {
                if ("revert".equals(event.getDescription())) {
                    notification.expire();
                    AdvancedSettings.setBoolean(CargoTestConsoleProperties.TEST_TOOL_WINDOW_SETTING_KEY, true);
                }
            }
        );
    }

    
    @Nonnull
    public static List<String> patchArgs(@Nonnull CargoCommandLine commandLine) {
        List<List<String>> splitResult = commandLine.splitOnDoubleDash();
        List<String> pre = new ArrayList<>(splitResult.get(0));
        List<String> post = new ArrayList<>(splitResult.get(1));

        String noFailFastOption = "--no-fail-fast";
        if (!pre.contains(noFailFastOption)) {
            pre.add(noFailFastOption);
        }

        String unstableOption = "-Z";
        if (!post.contains(unstableOption)) {
            post.add(unstableOption);
            post.add("unstable-options");
        }

        RunConfigUtil.addFormatJsonOption(post, "--format", "json");
        post.add("--show-output");

        if (post.isEmpty()) {
            return pre;
        } else {
            List<String> result = new ArrayList<>(pre);
            result.add("--");
            result.addAll(post);
            return result;
        }
    }
}
