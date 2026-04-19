/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.target;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.target.*;
import com.intellij.execution.target.value.TargetValue;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.util.execution.ParametersListUtil;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.runconfig.RsProcessHandler;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;

import java.util.Collections;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public final class TargetUtil {

    private static final Logger LOG = Logger.getInstance("org.rust.cargo.runconfig.target.Utils");

    private TargetUtil() {
    }

    @Nullable
    public static TargetEnvironmentConfiguration getTargetEnvironment(@NotNull CargoCommandConfiguration config) {
        if (!RunTargetsEnabled.get()) return null;
        String targetName = config.getDefaultTargetName();
        if (targetName == null) return null;
        return TargetEnvironmentsManager.getInstance(config.getProject()).getTargets().findByName(targetName);
    }

    @NotNull
    public static List<String> getLocalBuildArgsForRemoteRun(@NotNull CargoCommandConfiguration config) {
        if (hasRemoteTarget(config) && config.getBuildTarget().isLocal()) {
            TargetEnvironmentConfiguration targetEnv = getTargetEnvironment(config);
            RsLanguageRuntimeConfiguration runtime = targetEnv != null ? getLanguageRuntime(targetEnv) : null;
            String localBuildArgs = runtime != null ? runtime.getLocalBuildArgs() : null;
            if (localBuildArgs != null && !localBuildArgs.isEmpty()) {
                return ParametersListUtil.parse(localBuildArgs);
            }
        }
        return Collections.emptyList();
    }

    @Nullable
    public static RsLanguageRuntimeConfiguration getLanguageRuntime(@NotNull TargetEnvironmentConfiguration config) {
        return config.getRuntimes().findByType(RsLanguageRuntimeConfiguration.class);
    }

    @NotNull
    public static ProcessHandler startProcess(
        @NotNull GeneralCommandLine commandLine,
        @NotNull Project project,
        @Nullable TargetEnvironmentConfiguration config,
        boolean processColors,
        boolean uploadExecutable
    ) throws com.intellij.execution.ExecutionException {
        if (config == null) {
            RsProcessHandler handler = new RsProcessHandler(commandLine);
            ProcessTerminatedListener.attach(handler);
            return handler;
        }

        TargetEnvironmentRequest request = config.createEnvironmentRequest(project);
        RsCommandLineSetup setup = new RsCommandLineSetup(request);
        TargetedCommandLine targetCommandLine = toTargeted(commandLine, setup, uploadExecutable);
        ProgressIndicator progressIndicator = ProgressManager.getInstance().getProgressIndicator();
        if (progressIndicator == null) {
            progressIndicator = new EmptyProgressIndicator();
        }
        ProgressIndicator finalProgressIndicator = progressIndicator;
        TargetEnvironment environment = org.rust.openapiext.OpenApiUtil.computeWithCancelableProgress(
            project,
            RsBundle.message("progress.title.preparing.remote.environment"),
            () -> prepareEnvironment(request, setup, finalProgressIndicator)
        );
        Process process = environment.createProcess(targetCommandLine, finalProgressIndicator);

        String commandRepresentation = targetCommandLine.getCommandPresentation(environment);
        LOG.debug("Executing command: `" + commandRepresentation + "`");

        RsProcessHandler handler = new RsProcessHandler(process, commandRepresentation, targetCommandLine.getCharset(), processColors);
        ProcessTerminatedListener.attach(handler);
        return handler;
    }

    @NotNull
    private static TargetedCommandLine toTargeted(
        @NotNull GeneralCommandLine commandLine,
        @NotNull RsCommandLineSetup setup,
        boolean uploadExecutable
    ) {
        TargetedCommandLineBuilder commandLineBuilder = new TargetedCommandLineBuilder(setup.getRequest());
        commandLineBuilder.setCharset(commandLine.getCharset());

        TargetValue<String> targetedExePath = uploadExecutable
            ? setup.requestUploadIntoTarget(commandLine.getExePath())
            : TargetValue.fixed(commandLine.getExePath());
        commandLineBuilder.setExePath(targetedExePath);

        java.io.File workDirectory = commandLine.getWorkDirectory();
        if (workDirectory != null) {
            TargetValue<String> targetWorkingDirectory = setup.requestUploadIntoTarget(workDirectory.getAbsolutePath());
            commandLineBuilder.setWorkingDirectory(targetWorkingDirectory);
        }

        java.io.File inputFile = commandLine.getInputFile();
        if (inputFile != null) {
            TargetValue<String> targetInput = setup.requestUploadIntoTarget(inputFile.getAbsolutePath());
            commandLineBuilder.setInputFile(targetInput);
        }

        commandLineBuilder.addParameters(commandLine.getParametersList().getParameters());

        for (var entry : commandLine.getEnvironment().entrySet()) {
            commandLineBuilder.addEnvironmentVariable(entry.getKey(), entry.getValue());
        }

        RsLanguageRuntimeConfiguration runtime = setup.getRequest().getConfiguration() != null
            ? getLanguageRuntime(setup.getRequest().getConfiguration())
            : null;
        commandLineBuilder.addEnvironmentVariable("RUSTC",
            runtime != null ? StringUtil.nullize(runtime.getRustcPath(), true) : null);
        commandLineBuilder.addEnvironmentVariable("CARGO",
            runtime != null ? StringUtil.nullize(runtime.getCargoPath(), true) : null);

        return commandLineBuilder.build();
    }

    @NotNull
    private static TargetEnvironment prepareEnvironment(
        @NotNull TargetEnvironmentRequest request,
        @NotNull RsCommandLineSetup setup,
        @NotNull ProgressIndicator progressIndicator
    ) {
        TargetProgressIndicator targetProgressIndicator = new TargetProgressIndicator() {
            @Override
            public boolean isCanceled() {
                return progressIndicator.isCanceled();
            }

            @Override
            public void stop() {
                progressIndicator.cancel();
            }

            @Override
            public boolean isStopped() {
                return isCanceled();
            }

            @Override
            public void addText(@NotNull String text, @NotNull Key<?> key) {
                progressIndicator.setText2(text.trim());
            }
        };

        try {
            TargetEnvironment environment = request.prepareEnvironment(targetProgressIndicator);
            setup.provideEnvironment(environment, targetProgressIndicator);
            return environment;
        } catch (ProcessCanceledException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(
                new ExecutionException(
                    RsBundle.message("dialog.message.failed.to.prepare.remote.environment", e.getLocalizedMessage()), e
                )
            );
        }
    }

    /**
     * Checks whether the given configuration has a remote target.
     */
    public static boolean hasRemoteTarget(@NotNull CargoCommandConfiguration config) {
        return getTargetEnvironment(config) != null;
    }
}
