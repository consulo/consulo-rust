/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.toolchain;

import consulo.execution.RunManager;
import consulo.execution.executor.Executor;
import consulo.execution.ProgramRunnerUtil;
import consulo.execution.RunnerAndConfigurationSettings;
import consulo.execution.executor.DefaultRunExecutor;
import consulo.process.event.ProcessEvent;
import consulo.process.event.ProcessListener;
import consulo.project.Project;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.runner.ExecutionEnvironmentBuilder;
import consulo.execution.runner.ProgramRunner;
import consulo.project.ui.notification.NotificationType;
import jakarta.annotation.Nullable;
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

    protected abstract RunnerAndConfigurationSettings createRunConfiguration(RunManager runManager, @Nullable String name);

    protected RunnerAndConfigurationSettings createRunConfiguration(RunManager runManager) {
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
            } catch (consulo.process.ExecutionException e) {
                CompletableFuture<Boolean> failed = new CompletableFuture<>();
                failed.completeExceptionally(e);
                return failed;
            }
            CompletableFuture<Boolean> promise = new CompletableFuture<>();
            ProgramRunnerUtil.executeConfiguration(environment, true, true);
            // Consulo's executeConfiguration is synchronous-void; no descriptor callback hook
            promise.complete(true);
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
        RunManager runManager = RunManager.getInstance(project);
        RunnerAndConfigurationSettings configuration = createRunConfiguration(runManager, configurationName);
        if (saveConfiguration) {
            // RunManagerEx.setTemporaryConfiguration is platform-internal; the public API expresses
            // the same thing as "mark temporary, register, select".
            configuration.setTemporary(true);
            runManager.addConfiguration(configuration);
            runManager.setSelectedConfiguration(configuration);
        }

        ProgramRunner<?> runner = ProgramRunnerUtil.getRunner(executor.getId(), configuration);
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
