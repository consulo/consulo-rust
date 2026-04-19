/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.buildtool;

import com.intellij.execution.ExecutorRegistry;
import com.intellij.execution.RunManager;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.task.*;
import com.intellij.task.impl.ProjectModelBuildTaskImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.concurrency.*;
import org.rust.RsBundle;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.runconfig.CargoCommandRunner;
import org.rust.cargo.runconfig.RunConfigUtil;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.toolchain.CargoCommandLine;
import org.rust.cargo.toolchain.tools.CargoExtUtil;
import org.rust.cargo.util.RustCrateUtil;
import org.rust.ide.experiments.RsExperiments;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class CargoBuildTaskRunner extends ProjectTaskRunner {
    private static final Logger LOG = Logger.getInstance(CargoBuildTaskRunner.class);

    @NotNull
    @Override
    public Promise<Result> run(@NotNull Project project, @NotNull ProjectTaskContext context, @NotNull ProjectTask @NotNull ... tasks) {
        if (project.isDisposed()) {
            return Promises.rejectedPromise("Project is already disposed");
        }

        // Check for untrusted project
        boolean confirmed = com.intellij.openapi.application.ApplicationManager.getApplication()
            .isUnitTestMode() || org.rust.ide.notifications.NotificationUtils.confirmLoadingUntrustedProject(project);
        if (!confirmed) {
            return Promises.rejectedPromise(RsBundle.message("untrusted.project.notification.execution.error"));
        }

        com.intellij.execution.configurations.RunConfiguration configuration = context.getRunConfiguration();
        if (configuration instanceof CargoCommandConfiguration cargoConfig) {
            if (org.rust.cargo.runconfig.RunConfigUtil.getHasRemoteTarget(cargoConfig) ||
                !org.rust.openapiext.OpenApiUtil.isFeatureEnabled(RsExperiments.BUILD_TOOL_WINDOW)) {
                com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater(() ->
                    RunConfigUtil.buildProject(project)
                );
                return Promises.rejectedPromise();
            }
        }

        AsyncPromise<Result> resultPromise = new AsyncPromise<>();
        CompletableFuture<ProgressIndicator> waitingIndicator = new CompletableFuture<>();

        BackgroundableProjectTaskRunner queuedTask = new BackgroundableProjectTaskRunner(
            project, tasks, this, resultPromise, waitingIndicator
        );

        if (!org.rust.openapiext.OpenApiUtil.isHeadlessEnvironment()) {
            new WaitingTask(project, waitingIndicator, queuedTask.executionStarted).queue();
        }

        CargoBuildSessionsQueueManager.getInstance(project)
            .getBuildSessionsQueue()
            .run(queuedTask, ModalityState.defaultModalityState(), new EmptyProgressIndicator());

        return resultPromise;
    }

    @Override
    public boolean canRun(@NotNull ProjectTask projectTask) {
        if (projectTask instanceof ModuleFilesBuildTask) {
            return false;
        }
        if (projectTask instanceof ModuleBuildTask moduleBuildTask) {
            if (RustCrateUtil.getCargoProjectRoot(moduleBuildTask.getModule()) != null) return true;
            RunManager runManager = RunManager.getInstance(moduleBuildTask.getModule().getProject());
            com.intellij.execution.RunnerAndConfigurationSettings selected = runManager.getSelectedConfiguration();
            if (selected == null) return false;
            com.intellij.execution.configurations.RunConfiguration buildableElement = selected.getConfiguration();
            return buildableElement instanceof CargoCommandConfiguration cargoConfig &&
                CargoBuildManager.INSTANCE.isBuildToolWindowAvailable(cargoConfig);
        }
        if (projectTask instanceof ProjectModelBuildTask<?> modelTask) {
            Object buildableElement = modelTask.getBuildableElement();
            return buildableElement instanceof CargoBuildConfiguration cbConfig && cbConfig.getEnabled();
        }
        return false;
    }

    public List<ProjectTask> expandTask(ProjectTask task) {
        if (!(task instanceof ModuleBuildTask moduleBuildTask)) return List.of(task);

        Project project = moduleBuildTask.getModule().getProject();
        RunManager runManager = RunManager.getInstance(project);

        com.intellij.execution.RunnerAndConfigurationSettings selected = runManager.getSelectedConfiguration();
        if (selected != null && selected.getConfiguration() instanceof CargoCommandConfiguration selectedConfig) {
            CargoCommandConfiguration buildConfig = CargoBuildManager.INSTANCE.getBuildConfiguration(selectedConfig);
            if (buildConfig == null) return Collections.emptyList();
            ExecutionEnvironment env = CargoBuildManager.INSTANCE.createBuildEnvironment(buildConfig, null);
            if (env == null) return Collections.emptyList();
            CargoBuildConfiguration buildableElement = new CargoBuildConfiguration(buildConfig, env);
            return List.of(new ProjectModelBuildTaskImpl<>(buildableElement, moduleBuildTask.isIncrementalBuild()));
        }

        Collection<CargoProject> allProjects = CargoProjectServiceUtil.getCargoProjects(project).getAllProjects();
        if (allProjects.isEmpty()) return Collections.emptyList();

        com.intellij.execution.Executor executor = ExecutorRegistry.getInstance().getExecutorById(DefaultRunExecutor.EXECUTOR_ID);
        if (executor == null) return Collections.emptyList();
        ProgramRunner<?> runner = ProgramRunner.findRunnerById(CargoCommandRunner.RUNNER_ID);
        if (runner == null) return Collections.emptyList();

        List<String> additionalArguments = new ArrayList<>();
        org.rust.cargo.project.settings.RustProjectSettingsService settings = RsProjectSettingsServiceUtil.getRustSettings(project);
        additionalArguments.add("--all");
        if (settings.getCompileAllTargets()) {
            org.rust.cargo.toolchain.RsToolchainBase toolchain = settings.getToolchain();
            if (toolchain != null) {
                org.rust.cargo.toolchain.tools.Cargo cargo = CargoExtUtil.cargo(toolchain);
                if (cargo.checkSupportForBuildCheckAllTargets()) {
                    additionalArguments.add("--all-targets");
                }
            }
        }

        List<ProjectTask> result = new ArrayList<>();
        for (CargoProject cargoProject : allProjects) {
            CargoCommandLine cmdLine = CargoCommandLine.forProject(cargoProject, "build", additionalArguments);
            com.intellij.execution.RunnerAndConfigurationSettings runSettings = RunConfigUtil.createCargoCommandRunConfiguration(runManager, cmdLine, null);
            ExecutionEnvironment env = new ExecutionEnvironment(executor, runner, runSettings, project);
            com.intellij.execution.configurations.RunConfiguration config = runSettings.getConfiguration();
            if (!(config instanceof CargoCommandConfiguration cargoConfig)) continue;
            cargoConfig.setEmulateTerminal(false);
            CargoBuildConfiguration buildableElement = new CargoBuildConfiguration(cargoConfig, env);
            result.add(new ProjectModelBuildTaskImpl<>(buildableElement, moduleBuildTask.isIncrementalBuild()));
        }
        return result;
    }

    public Promise<Result> executeTask(ProjectTask task) {
        if (!(task instanceof ProjectModelBuildTask<?> modelBuildTask)) {
            return Promises.resolvedPromise(TaskRunnerResults.ABORTED);
        }

        CargoBuildConfiguration buildConfiguration = (CargoBuildConfiguration) modelBuildTask.getBuildableElement();

        if (!modelBuildTask.isIncrementalBuild()) {
            CargoCommandConfiguration config = buildConfiguration.getConfiguration();
            CargoProject cargoProject = CargoCommandConfiguration.findCargoProject(
                config.getProject(), config.getCommand(), config.getWorkingDirectory()
            );
            if (cargoProject != null) {
                try {
                    Future<Boolean> cleanFuture = CargoBuildManager.INSTANCE.clean(cargoProject);
                    Result cleanResult = cleanFuture.get() ? TaskRunnerResults.SUCCESS : TaskRunnerResults.FAILURE;
                    if (cleanResult.hasErrors()) {
                        return Promises.resolvedPromise(cleanResult);
                    }
                } catch (ExecutionException | InterruptedException e) {
                    return Promises.resolvedPromise(TaskRunnerResults.FAILURE);
                }
            }
        }

        Result result;
        try {
            Future<CargoBuildResult> buildFuture = CargoBuildManager.INSTANCE.build(buildConfiguration);
            CargoBuildResult buildResult = buildFuture.get();
            if (buildResult.canceled()) {
                result = TaskRunnerResults.ABORTED;
            } else if (buildResult.succeeded()) {
                result = TaskRunnerResults.SUCCESS;
            } else {
                result = TaskRunnerResults.FAILURE;
            }
        } catch (ExecutionException | InterruptedException e) {
            result = TaskRunnerResults.FAILURE;
        }

        AsyncPromise<Result> promise = new AsyncPromise<>();
        promise.setResult(result);
        return promise;
    }

    private static class BackgroundableProjectTaskRunner extends Task.Backgroundable {
        private final ProjectTask[] tasks;
        private final CargoBuildTaskRunner parentRunner;
        private final AsyncPromise<Result> totalPromise;
        private final Future<ProgressIndicator> waitingIndicator;
        final CompletableFuture<Boolean> executionStarted = new CompletableFuture<>();

        BackgroundableProjectTaskRunner(
            Project project,
            ProjectTask[] tasks,
            CargoBuildTaskRunner parentRunner,
            AsyncPromise<Result> totalPromise,
            Future<ProgressIndicator> waitingIndicator
        ) {
            super(project, RsBundle.message("progress.title.building"), true);
            this.tasks = tasks;
            this.parentRunner = parentRunner;
            this.totalPromise = totalPromise;
            this.waitingIndicator = waitingIndicator;
        }

        @Override
        public void run(@NotNull ProgressIndicator indicator) {
            if (!waitForStart()) {
                if (totalPromise.getState() == Promise.State.PENDING) {
                    totalPromise.cancel();
                }
                return;
            }

            Collection<ProjectTask> allTasks = collectTasks(tasks);
            if (allTasks.isEmpty()) {
                totalPromise.setResult(TaskRunnerResults.FAILURE);
                return;
            }

            try {
                for (ProjectTask task : allTasks) {
                    Promise<Result> promise = parentRunner.executeTask(task);
                    if (promise.blockingGet(Integer.MAX_VALUE) != TaskRunnerResults.SUCCESS) {
                        totalPromise.setResult(TaskRunnerResults.FAILURE);
                        break;
                    }
                }

                if (totalPromise.getState() == Promise.State.PENDING) {
                    totalPromise.setResult(TaskRunnerResults.SUCCESS);
                }
            } catch (CancellationException e) {
                totalPromise.setResult(TaskRunnerResults.ABORTED);
                throw new ProcessCanceledException(e);
            } catch (Throwable e) {
                LOG.error(e);
                totalPromise.setResult(TaskRunnerResults.FAILURE);
            }
        }

        private boolean waitForStart() {
            if (org.rust.openapiext.OpenApiUtil.isHeadlessEnvironment()) return true;
            try {
                boolean cancelled = waitingIndicator.get().isCanceled();
                executionStarted.complete(true);
                return !cancelled;
            } catch (InterruptedException e) {
                totalPromise.setResult(TaskRunnerResults.ABORTED);
                throw new ProcessCanceledException(e);
            } catch (CancellationException e) {
                totalPromise.setResult(TaskRunnerResults.ABORTED);
                throw new ProcessCanceledException(e);
            } catch (Throwable e) {
                LOG.error(e);
                totalPromise.setResult(TaskRunnerResults.FAILURE);
                throw new ProcessCanceledException(e);
            }
        }

        private Collection<ProjectTask> collectTasks(ProjectTask[] tasks) {
            List<List<ProjectTask>> expandedTasks = new ArrayList<>();
            for (ProjectTask task : tasks) {
                if (parentRunner.canRun(task)) {
                    expandedTasks.add(parentRunner.expandTask(task));
                }
            }
            if (expandedTasks.stream().anyMatch(List::isEmpty)) return Collections.emptyList();
            return expandedTasks.stream().flatMap(Collection::stream).collect(Collectors.toList());
        }
    }

    private static class WaitingTask extends Task.Backgroundable {
        private final CompletableFuture<ProgressIndicator> waitingIndicator;
        private final Future<Boolean> executionStarted;

        WaitingTask(Project project, CompletableFuture<ProgressIndicator> waitingIndicator, Future<Boolean> executionStarted) {
            super(project, RsBundle.message("progress.text.waiting.for.current.build.to.finish"), true);
            this.waitingIndicator = waitingIndicator;
            this.executionStarted = executionStarted;
        }

        @Override
        public void run(@NotNull ProgressIndicator indicator) {
            waitingIndicator.complete(indicator);
            try {
                while (true) {
                    indicator.checkCanceled();
                    try {
                        executionStarted.get(100, TimeUnit.MILLISECONDS);
                        break;
                    } catch (TimeoutException ignore) {
                    }
                }
            } catch (CancellationException e) {
                throw new ProcessCanceledException(e);
            } catch (InterruptedException e) {
                throw new ProcessCanceledException(e);
            } catch (ExecutionException e) {
                LOG.error(e);
                throw new ProcessCanceledException(e);
            }
        }
    }
}
