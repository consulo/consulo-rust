/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.buildtool;
import consulo.execution.RunnerAndConfigurationSettings;

import consulo.application.Application;
import consulo.ide.impl.idea.build.BuildContentDescriptor;
import consulo.build.ui.BuildDescriptor;
import consulo.build.ui.progress.BuildProgressListener;
import consulo.build.ui.DefaultBuildDescriptor;
import consulo.build.ui.event.BuildEventFactory;
import consulo.build.ui.event.EventResult;
import consulo.build.ui.event.FinishBuildEvent;
import consulo.build.ui.event.StartBuildEvent;
import consulo.execution.executor.ExecutorRegistry;
import consulo.ide.impl.idea.execution.actions.StopProcessAction;
import consulo.process.ProcessHandlerStopper;
import consulo.process.event.ProcessEvent;
import consulo.process.ProcessHandler;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.ExecutionUtil;
import consulo.application.AllIcons;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.project.Project;
import consulo.project.DumbService;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.cargo.CargoConstants;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.runconfig.RunConfigUtil;

import javax.swing.*;
import java.nio.file.Path;

@SuppressWarnings("UnstableApiUsage")
public class CargoBuildAdapter extends CargoBuildAdapterBase {
    private final CargoBuildContext context;

    public CargoBuildAdapter(CargoBuildContext context, BuildProgressListener buildProgressListener) {
        super(context, buildProgressListener);
        this.context = context;

        ProcessHandler processHandler = context.getProcessHandler();
        if (processHandler == null) {
            throw new IllegalStateException("Process handler can't be null");
        }
        Utils.notifyProcessStarted(context.getEnvironment(), processHandler);

        BuildContentDescriptor buildContentDescriptor = new BuildContentDescriptor(null, null, new JComponent() {}, RsBundle.message("build"));
        boolean activateToolWindow = Utils.isActivateToolWindowBeforeRun(context.getEnvironment());
        buildContentDescriptor.setActivateToolWindowWhenAdded(activateToolWindow);
        buildContentDescriptor.setActivateToolWindowWhenFailed(activateToolWindow);
        // setNavigateToError is IntelliJ-only; omitted

        DefaultBuildDescriptor descriptor = new DefaultBuildDescriptor(
            context.getBuildId(),
            LocalizeValue.of(RsBundle.message("build.event.title.run.cargo.command")),
            context.getWorkingDirectory().toString(),
            context.getStarted()
        )
            .withContentDescriptor(() -> buildContentDescriptor)
            .withRestartAction(createRerunAction(processHandler, context.getEnvironment()))
            .withRestartAction(createStopAction(processHandler));

        for (consulo.execution.ui.console.Filter filter : RunConfigUtil.createFilters(context.getCargoProject())) {
            descriptor.withExecutionFilter(filter);
        }

        BuildEventFactory factory = Application.get().getInstance(BuildEventFactory.class);
        StartBuildEvent buildStarted = factory.createStartBuildEvent(
            descriptor,
            LocalizeValue.of(RsBundle.message("build.event.message.running", context.getTaskName()))
        );
        buildProgressListener.onEvent(context.getBuildId(), buildStarted);
    }

    @Override
    public void onBuildOutputReaderFinish(
        ProcessEvent event,
        boolean isSuccess,
        boolean isCanceled,
        Throwable error
    ) {
        BuildEventFactory factory = Application.get().getInstance(BuildEventFactory.class);
        String status;
        EventResult result;
        if (isCanceled) {
            status = "canceled";
            result = factory.createSkippedResult();
        } else if (isSuccess) {
            status = "successful";
            result = factory.createSuccessResult();
        } else {
            status = "failed";
            result = factory.createFailureResult(error);
        }

        FinishBuildEvent buildFinished = factory.createFinishBuildEvent(
            context.getBuildId(),
            null,
            System.currentTimeMillis(),
            LocalizeValue.of(RsBundle.message("build.event.message.", context.getTaskName(), status)),
            result
        );
        buildProgressListener.onEvent(context.getBuildId(), buildFinished);
        context.finished(isSuccess);

        Utils.notifyProcessTerminated(context.getEnvironment(), event.getProcessHandler(), event.getExitCode());

        Path targetPath = context.getWorkingDirectory().resolve(CargoConstants.ProjectLayout.target);
        VirtualFile targetDir = consulo.virtualFileSystem.LocalFileSystem.getInstance().findFileByNioFile(targetPath);
        if (targetDir == null) return;
        VirtualFileUtil.markDirtyAndRefresh(true, true, true, targetDir);
    }

    @Override
    public void processWillTerminate(@Nonnull ProcessEvent event, boolean willBeDestroyed) {
        Utils.notifyProcessTerminating(context.getEnvironment(), event.getProcessHandler());
    }

    private static StopProcessAction createStopAction(ProcessHandler processHandler) {
        return new StopProcessAction(consulo.localize.LocalizeValue.of("Stop"), consulo.localize.LocalizeValue.of("Stop"), processHandler);
    }

    private static RestartProcessAction createRerunAction(ProcessHandler processHandler, ExecutionEnvironment environment) {
        return new RestartProcessAction(processHandler, environment);
    }

    private static class RestartProcessAction extends DumbAwareAction {
        private final ProcessHandler processHandler;
        private final ExecutionEnvironment environment;

        RestartProcessAction(ProcessHandler processHandler, ExecutionEnvironment environment) {
            this.processHandler = processHandler;
            this.environment = environment;
        }

        private boolean isEnabled() {
            Project project = environment.getProject();
            consulo.execution.RunnerAndConfigurationSettings settings = environment.getRunnerAndConfigurationSettings();
            return (!DumbService.isDumb(project) || settings == null || settings.getType().isDumbAware()) &&
                !ExecutorRegistry.getInstance().isStarting(environment) &&
                !processHandler.isProcessTerminating();
        }

        @Override
        public void update(@Nonnull AnActionEvent event) {
            consulo.ui.ex.action.Presentation presentation = event.getPresentation();
            presentation.setText(RsBundle.message("action.rerun.text", StringUtil.escapeMnemonics(environment.getRunProfile().getName())));
            presentation.setIcon(processHandler.isProcessTerminated() ? AllIcons.Actions.Compile : AllIcons.Actions.Restart);
            presentation.setEnabled(isEnabled());
        }

        @Override
        public void actionPerformed(@Nonnull AnActionEvent event) {
            ProcessHandlerStopper.stop(processHandler);
            ExecutionUtil.restart(environment);
        }
    }
}
