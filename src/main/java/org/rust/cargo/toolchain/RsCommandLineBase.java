/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain;

import com.intellij.execution.Executor;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.openapi.project.Project;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.notification.NotificationType;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.ide.notifications.RsNotifications;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public abstract class RsCommandLineBase {

    public abstract String getCommand();

    public abstract Path getWorkingDirectory();

    @Nullable
    public abstract File getRedirectInputFrom();

    public abstract List<String> getAdditionalArguments();

    public abstract boolean getEmulateTerminal();

    protected abstract String getExecutableName();

    protected abstract RunnerAndConfigurationSettings createRunConfiguration(RunManagerEx runManager, @Nullable String name);

    protected RunnerAndConfigurationSettings createRunConfiguration(RunManagerEx runManager) {
        return createRunConfiguration(runManager, null);
    }

    public void run(
        CargoProject cargoProject,
        String presentableName,
        boolean saveConfiguration,
        Executor executor
    ) {
        runInner(cargoProject, presentableName, saveConfiguration, executor, (configuration, finalExecutor) -> {
            ProgramRunnerUtil.executeConfiguration(configuration, finalExecutor);
            return CompletableFuture.completedFuture(true);
        });
    }

    public void run(CargoProject cargoProject, String presentableName, boolean saveConfiguration) {
        run(cargoProject, presentableName, saveConfiguration, DefaultRunExecutor.getRunExecutorInstance());
    }

    public void run(CargoProject cargoProject, String presentableName) {
        run(cargoProject, presentableName, true);
    }

    public void run(CargoProject cargoProject) {
        run(cargoProject, getCommand());
    }

    public Future<Boolean> runAsync(
        CargoProject cargoProject,
        String presentableName,
        boolean saveConfiguration,
        Executor executor
    ) {
        return runInner(cargoProject, presentableName, saveConfiguration, executor, (configuration, finalExecutor) -> {
            ExecutionEnvironment environment;
            try {
                environment = ExecutionEnvironmentBuilder.create(finalExecutor, configuration).build();
            } catch (com.intellij.execution.ExecutionException e) {
                CompletableFuture<Boolean> failed = new CompletableFuture<>();
                failed.completeExceptionally(e);
                return failed;
            }
            CompletableFuture<Boolean> promise = new CompletableFuture<>();
            ProgramRunnerUtil.executeConfigurationAsync(environment, true, true, descriptor -> {
                if (descriptor.getProcessHandler() != null) {
                    descriptor.getProcessHandler().addProcessListener(new ProcessListener() {
                        @Override
                        public void processTerminated(ProcessEvent event) {
                            promise.complete(event.getExitCode() == 0);
                        }
                    });
                }
            });
            return promise;
        });
    }

    public Future<Boolean> runAsync(CargoProject cargoProject, String presentableName, boolean saveConfiguration) {
        return runAsync(cargoProject, presentableName, saveConfiguration, DefaultRunExecutor.getRunExecutorInstance());
    }

    public Future<Boolean> runAsync(CargoProject cargoProject, String presentableName) {
        return runAsync(cargoProject, presentableName, true);
    }

    public Future<Boolean> runAsync(CargoProject cargoProject) {
        return runAsync(cargoProject, getCommand());
    }

    @FunctionalInterface
    private interface RunAction<T> {
        Future<T> run(RunnerAndConfigurationSettings configuration, Executor executor);
    }

    private <T> Future<T> runInner(
        CargoProject cargoProject,
        String presentableName,
        boolean saveConfiguration,
        Executor executor,
        RunAction<T> doRun
    ) {
        Project project = cargoProject.getProject();
        String configurationName;
        if (CargoProjectServiceUtil.getCargoProjects(project).getAllProjects().size() > 1) {
            configurationName = presentableName + " [" + cargoProject.getPresentableName() + "]";
        } else {
            configurationName = presentableName;
        }
        RunManagerEx runManager = RunManagerEx.getInstanceEx(project);
        RunnerAndConfigurationSettings configuration = createRunConfiguration(runManager, configurationName);
        if (saveConfiguration) {
            runManager.setTemporaryConfiguration(configuration);
        }

        ProgramRunner<?> runner = ProgramRunner.getRunner(executor.getId(), configuration.getConfiguration());
        Executor finalExecutor;
        if (runner == null) {
            RsNotifications.INSTANCE.pluginNotifications()
                .createNotification(
                    RsBundle.message("notification.0.action.is.not.available.for.1.command", executor.getActionName(), getExecutableName() + " " + getCommand()),
                    NotificationType.WARNING
                )
                .notify(project);
            finalExecutor = DefaultRunExecutor.getRunExecutorInstance();
        } else {
            finalExecutor = executor;
        }

        return doRun.run(configuration, finalExecutor);
    }
}
