/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.buildtool;

import com.intellij.build.BuildContentDescriptor;
import com.intellij.build.BuildProgressListener;
import com.intellij.build.DefaultBuildDescriptor;
import com.intellij.build.events.impl.*;
import com.intellij.execution.ExecutorRegistry;
import com.intellij.execution.actions.StopProcessAction;
import com.intellij.execution.impl.ExecutionManagerImpl;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
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
        buildContentDescriptor.setNavigateToError(RsProjectSettingsServiceUtil.getRustSettings(context.getProject()).getAutoShowErrorsInEditor());

        DefaultBuildDescriptor descriptor = new DefaultBuildDescriptor(
            context.getBuildId(),
            RsBundle.message("build.event.title.run.cargo.command"),
            context.getWorkingDirectory().toString(),
            context.getStarted()
        )
            .withContentDescriptor(() -> buildContentDescriptor)
            .withRestartAction(createRerunAction(processHandler, context.getEnvironment()))
            .withRestartAction(createStopAction(processHandler));

        for (com.intellij.execution.filters.Filter filter : RunConfigUtil.createFilters(context.getCargoProject())) {
            descriptor.withExecutionFilter(filter);
        }

        StartBuildEventImpl buildStarted = new StartBuildEventImpl(
            descriptor,
            RsBundle.message("build.event.message.running", context.getTaskName())
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
        String status;
        Object result;
        if (isCanceled) {
            status = "canceled";
            result = new SkippedResultImpl();
        } else if (isSuccess) {
            status = "successful";
            result = new SuccessResultImpl();
        } else {
            status = "failed";
            result = new FailureResultImpl(error);
        }

        FinishBuildEventImpl buildFinished = new FinishBuildEventImpl(
            context.getBuildId(),
            null,
            System.currentTimeMillis(),
            RsBundle.message("build.event.message.", context.getTaskName(), status),
            (com.intellij.build.events.EventResult) result
        );
        buildProgressListener.onEvent(context.getBuildId(), buildFinished);
        context.finished(isSuccess);

        Utils.notifyProcessTerminated(context.getEnvironment(), event.getProcessHandler(), event.getExitCode());

        Path targetPath = context.getWorkingDirectory().resolve(CargoConstants.ProjectLayout.target);
        VirtualFile targetDir = VfsUtil.findFile(targetPath, true);
        if (targetDir == null) return;
        VfsUtil.markDirtyAndRefresh(true, true, true, targetDir);
    }

    @Override
    public void processWillTerminate(@NotNull ProcessEvent event, boolean willBeDestroyed) {
        Utils.notifyProcessTerminating(context.getEnvironment(), event.getProcessHandler());
    }

    private static StopProcessAction createStopAction(ProcessHandler processHandler) {
        return new StopProcessAction("Stop", "Stop", processHandler);
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
            com.intellij.execution.RunnerAndConfigurationSettings settings = environment.getRunnerAndConfigurationSettings();
            return (!DumbService.isDumb(project) || settings == null || settings.getType().isDumbAware()) &&
                !ExecutorRegistry.getInstance().isStarting(environment) &&
                !processHandler.isProcessTerminating();
        }

        @Override
        public void update(@NotNull AnActionEvent event) {
            com.intellij.openapi.actionSystem.Presentation presentation = event.getPresentation();
            presentation.setText(RsBundle.message("action.rerun.text", StringUtil.escapeMnemonics(environment.getRunProfile().getName())));
            presentation.setIcon(processHandler.isProcessTerminated() ? AllIcons.Actions.Compile : AllIcons.Actions.Restart);
            presentation.setEnabled(isEnabled());
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent event) {
            ExecutionManagerImpl.stopProcess(processHandler);
            ExecutionUtil.restart(environment);
        }
    }
}
