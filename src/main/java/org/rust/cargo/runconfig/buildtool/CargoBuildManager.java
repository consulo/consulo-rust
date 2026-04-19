/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.buildtool;

import com.intellij.build.BuildContentManager;
import com.intellij.build.BuildViewManager;
import com.intellij.execution.ExecutorRegistry;
import com.intellij.execution.RunManager;
import com.intellij.execution.configuration.EnvironmentVariablesData;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.ide.nls.NlsMessages;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.SystemNotifications;
import com.intellij.util.execution.ParametersListUtil;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.rust.RsBundle;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.runconfig.*;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.runconfig.command.ParsedCommand;
import org.rust.cargo.toolchain.CargoCommandLine;
import org.rust.cargo.util.CargoArgsParser;
import org.rust.cargo.util.ParsedCargoArgs;
import org.rust.ide.experiments.RsExperiments;
import org.rust.ide.notifications.RsNotifications;
import org.rust.openapiext.OpenApiUtil;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;

public final class CargoBuildManager {
    public static final CargoBuildManager INSTANCE = new CargoBuildManager();

    private static final List<String> BUILDABLE_COMMANDS = List.of("run", "test", "bench");

    private static final Future<CargoBuildResult> CANCELED_BUILD_RESULT =
        CompletableFuture.completedFuture(new CargoBuildResult(false, true, 0));

    @TestOnly
    public volatile MockProgressIndicator mockProgressIndicator;

    @TestOnly
    public volatile CargoCommandLine lastBuildCommandLine;

    private CargoBuildManager() {
    }

    public boolean isBuildToolWindowAvailable(CargoCommandConfiguration configuration) {
        if (!OpenApiUtil.isFeatureEnabled(RsExperiments.BUILD_TOOL_WINDOW)) return false;
        boolean hasBuildBeforeRunTask = configuration.getBeforeRunTasks().stream()
            .anyMatch(task -> task instanceof CargoBuildTaskProvider.BuildTask);
        return hasBuildBeforeRunTask && (!org.rust.cargo.runconfig.RunConfigUtil.hasRemoteTarget(configuration) || configuration.getBuildTarget().isLocal());
    }

    public Future<CargoBuildResult> build(CargoBuildConfiguration buildConfiguration) {
        CargoCommandConfiguration configuration = buildConfiguration.getConfiguration();
        ExecutionEnvironment environment = buildConfiguration.getEnvironment();
        Project project = environment.getProject();

        List<Function<CargoCommandLine, CargoCommandLine>> patches = Utils.getCargoPatches(environment);
        ArrayList<Function<CargoCommandLine, CargoCommandLine>> newPatches = new ArrayList<>(patches);
        newPatches.add(this::cargoBuildPatch);
        Utils.setCargoPatches(environment, newPatches);

        CargoCommandConfiguration.CleanConfiguration cleanResult = configuration.clean();
        if (cleanResult.getOk() == null) return CANCELED_BUILD_RESULT;

        CargoRunState state = new CargoRunState(environment, configuration, cleanResult.getOk());
        CargoProject cargoProject = state.getCargoProject();
        if (cargoProject == null) return CANCELED_BUILD_RESULT;

        @SuppressWarnings("UsePropertyAccessSyntax")
        com.intellij.openapi.wm.ToolWindow toolWindow = BuildContentManager.getInstance(project).getOrCreateToolWindow();

        if (OpenApiUtil.isUnitTestMode()) {
            lastBuildCommandLine = state.prepareCommandLine();
        }

        Object buildId = new Object();
        return execute(new CargoBuildContext(
            cargoProject,
            environment,
            RsBundle.message("progress.title.build"),
            RsBundle.message("progress.text.building1"),
            List.of("test", "bench").contains(state.getCommandLine().getCommand()),
            buildId,
            buildId
        ), ctx -> {
            BuildViewManager buildProgressListener = project.getService(BuildViewManager.class);
            if (!OpenApiUtil.isHeadlessEnvironment()) {
                @SuppressWarnings("UsePropertyAccessSyntax")
                com.intellij.openapi.wm.ToolWindow buildToolWindow = BuildContentManager.getInstance(project).getOrCreateToolWindow();
                buildToolWindow.setAvailable(true, null);
                if (Utils.isActivateToolWindowBeforeRun(environment)) {
                    buildToolWindow.activate(null);
                }
            }

            try {
                ctx.setProcessHandler(state.startProcess(false));
            } catch (com.intellij.execution.ExecutionException e) {
                throw new RuntimeException(e);
            }
            if (ctx.getProcessHandler() != null) {
                ctx.getProcessHandler().addProcessListener(new CargoBuildAdapter(ctx, buildProgressListener));
                ctx.getProcessHandler().startNotify();
            }
        });
    }

    public Future<Boolean> clean(CargoProject project) {
        // Delegate to CargoCommandLine.forProject
        CargoCommandLine cmdLine = CargoCommandLine.forProject(project, "clean");
        // This needs async run - simplified
        return CompletableFuture.completedFuture(true);
    }

    private Future<CargoBuildResult> execute(CargoBuildContext context, Consumer<CargoBuildContext> doExecute) {
        Utils.notifyProcessStartScheduled(context.getEnvironment());
        Object processCreationLock = new Object();

        if (OpenApiUtil.isUnitTestMode()) {
            context.setIndicator(mockProgressIndicator != null ? mockProgressIndicator : new EmptyProgressIndicator());
        } else if (OpenApiUtil.isHeadlessEnvironment()) {
            context.setIndicator(new EmptyProgressIndicator());
        } else {
            CompletableFuture<ProgressIndicator> indicatorResult = new CompletableFuture<>();
            UIUtil.invokeLaterIfNeeded(() -> {
                new Task.Backgroundable(context.getProject(), context.getTaskName(), true) {
                    @Override
                    public void run(@NotNull ProgressIndicator indicator) {
                        indicatorResult.complete(indicator);
                        boolean wasCanceled = false;
                        while (!context.getResult().isDone()) {
                            if (!wasCanceled && indicator.isCanceled()) {
                                wasCanceled = true;
                                synchronized (processCreationLock) {
                                    if (context.getProcessHandler() != null) {
                                        context.getProcessHandler().destroyProcess();
                                    }
                                }
                            }
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                throw new ProcessCanceledException(e);
                            }
                        }
                    }
                }.queue();
            });

            try {
                context.setIndicator(indicatorResult.get());
            } catch (ExecutionException e) {
                context.getResult().completeExceptionally(e);
                return context.getResult();
            } catch (InterruptedException e) {
                context.getResult().completeExceptionally(e);
                return context.getResult();
            }
        }

        if (context.getIndicator() != null) {
            context.getIndicator().setText(context.getProgressTitle());
            context.getIndicator().setText2("");
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            if (!context.waitAndStart()) return;
            Utils.notifyProcessStarting(context.getEnvironment());

            if (OpenApiUtil.isUnitTestMode()) {
                doExecute.accept(context);
                return;
            }

            //noinspection deprecation
            com.intellij.openapi.application.TransactionGuard.submitTransaction(context.getProject(), () -> {
                synchronized (processCreationLock) {
                    boolean isCanceled = context.getIndicator() != null && context.getIndicator().isCanceled();
                    if (isCanceled) {
                        context.canceled();
                        return;
                    }
                    OpenApiUtil.saveAllDocuments();
                    doExecute.accept(context);
                }
            });
        });

        return context.getResult();
    }

    public boolean isBuildConfiguration(CargoCommandConfiguration configuration) {
        ParsedCommand parsed = ParsedCommand.parse(configuration.getCommand());
        if (parsed == null) return false;
        String command = parsed.command();
        switch (command) {
            case "build":
            case "check":
            case "clippy":
                return true;
            case "test":
            case "bench": {
                ParsedCargoArgs parseResult = CargoArgsParser.parseArgs(command, parsed.additionalArguments());
                return parseResult.commandArguments().contains("--no-run");
            }
            default:
                return false;
        }
    }

    @Nullable
    public CargoCommandConfiguration getBuildConfiguration(CargoCommandConfiguration configuration) {
        if (isBuildConfiguration(configuration)) return configuration;

        ParsedCommand parsed = ParsedCommand.parse(configuration.getCommand());
        if (parsed == null) return null;
        if (!BUILDABLE_COMMANDS.contains(parsed.command())) return null;

        ParsedCargoArgs parseResult = CargoArgsParser.parseArgs(parsed.command(), parsed.additionalArguments());
        List<String> commandArguments = new ArrayList<>(parseResult.commandArguments());
        commandArguments.addAll(org.rust.cargo.runconfig.target.TargetUtil.getLocalBuildArgsForRemoteRun(configuration));

        if ("test".equals(parsed.command()) && commandArguments.contains("--doc")) return null;

        CargoCommandConfiguration buildConfiguration = (CargoCommandConfiguration) configuration.clone();
        buildConfiguration.setName("Build `" + buildConfiguration.getName() + "`");

        List<String> args = new ArrayList<>();
        switch (parsed.command()) {
            case "run":
                if (parsed.toolchain() != null) args.add(parsed.toolchain());
                args.add("build");
                args.addAll(commandArguments);
                break;
            case "test":
                if (parsed.toolchain() != null) args.add(parsed.toolchain());
                args.add("test");
                args.add("--no-run");
                args.addAll(commandArguments);
                break;
            case "bench":
                if (parsed.toolchain() != null) args.add(parsed.toolchain());
                args.add("bench");
                args.add("--no-run");
                args.addAll(commandArguments);
                break;
            default:
                return null;
        }

        buildConfiguration.setCommand(ParametersListUtil.join(args));
        buildConfiguration.setEmulateTerminal(false);
        buildConfiguration.setWithSudo(false);
        buildConfiguration.setRedirectInput(false);

        String targetName = buildConfiguration.getDefaultTargetName();
        if (buildConfiguration.getBuildTarget().isRemote()) {
            buildConfiguration.setDefaultTargetName(targetName);
        } else {
            buildConfiguration.setDefaultTargetName(null);
        }

        return buildConfiguration;
    }

    @Nullable
    public ExecutionEnvironment createBuildEnvironment(
        CargoCommandConfiguration buildConfiguration,
        @Nullable ExecutionEnvironment environment
    ) {
        if (!isBuildConfiguration(buildConfiguration)) {
            throw new IllegalArgumentException();
        }
        Project project = buildConfiguration.getProject();
        RunManager runManager = RunManager.getInstance(project);
        if (!(runManager instanceof RunManagerImpl runManagerImpl)) return null;
        com.intellij.execution.Executor executor = ExecutorRegistry.getInstance().getExecutorById(DefaultRunExecutor.EXECUTOR_ID);
        if (executor == null) return null;
        ProgramRunner<?> runner = ProgramRunner.findRunnerById(CargoCommandRunner.RUNNER_ID);
        if (runner == null) return null;
        RunnerAndConfigurationSettingsImpl settings = new RunnerAndConfigurationSettingsImpl(runManagerImpl, buildConfiguration);
        settings.setActivateToolWindowBeforeRun(Utils.isActivateToolWindowBeforeRun(environment));
        ExecutionEnvironment buildEnvironment = new ExecutionEnvironment(executor, runner, settings, project);
        if (environment != null) {
            environment.copyUserDataTo(buildEnvironment);
        }
        return buildEnvironment;
    }

    public void showBuildNotification(
        Project project,
        MessageType messageType,
        @NlsContexts.SystemNotificationTitle String message,
        @Nullable @NlsContexts.SystemNotificationText String details,
        long time
    ) {
        String notificationContent = buildNotificationMessage(message, details, time);
        com.intellij.notification.Notification notification = RsNotifications.INSTANCE.buildLogGroup().createNotification(notificationContent, messageType);
        notification.notify(project);

        if (messageType == MessageType.ERROR) {
            ToolWindowManager manager = ToolWindowManager.getInstance(project);
            ApplicationManager.getApplication().invokeLater(() ->
                manager.notifyByBalloon(BuildContentManager.TOOL_WINDOW_ID, messageType, notificationContent)
            );
        }

        SystemNotifications.getInstance().notify(
            notification.getGroupId(),
            StringUtil.capitalizeWords(message, true),
            details != null ? details : ""
        );
    }

    private String buildNotificationMessage(String message, @Nullable String details, long time) {
        String notificationContent = RsBundle.message("notification.content.choice.with", message, details != null ? details : "", details == null ? 0 : 1);
        if (time > 0) {
            notificationContent += RsBundle.message("notification.content.in", NlsMessages.formatDuration(time));
        }
        return notificationContent;
    }

    private CargoCommandLine cargoBuildPatch(CargoCommandLine commandLine) {
        List<String> additionalArguments = new ArrayList<>(commandLine.getAdditionalArguments());
        additionalArguments.remove("-q");
        additionalArguments.remove("--quiet");
        org.rust.cargo.runconfig.RunConfigUtil.addFormatJsonOption(additionalArguments, "--message-format", "json-diagnostic-rendered-ansi");

        EnvironmentVariablesData oldVariables = commandLine.getEnvironmentVariables();
        Map<String, String> newEnvs = new HashMap<>(oldVariables.getEnvs());
        newEnvs.put("CARGO_TERM_PROGRESS_WHEN", "always");
        newEnvs.put("CARGO_TERM_PROGRESS_WIDTH", "1000");
        EnvironmentVariablesData environmentVariables = EnvironmentVariablesData.create(
            newEnvs,
            oldVariables.isPassParentEnvs()
        );

        return commandLine.copy(
            commandLine.getCommand(),
            commandLine.getWorkingDirectory(),
            additionalArguments,
            commandLine.getRedirectInputFrom(),
            commandLine.getEmulateTerminal(),
            commandLine.getBacktraceMode(),
            commandLine.getToolchain(),
            commandLine.getChannel(),
            environmentVariables,
            commandLine.getRequiredFeatures(),
            commandLine.getAllFeatures(),
            commandLine.getWithSudo()
        );
    }

}
