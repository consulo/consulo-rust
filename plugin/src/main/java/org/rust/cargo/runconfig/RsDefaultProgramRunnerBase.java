/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig;
import consulo.execution.ExecutionResult;

import consulo.process.ExecutionException;
import consulo.execution.ExecutionManager;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.configuration.RunnerSettings;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.runner.ProgramRunner;
import consulo.execution.runner.RunContentBuilder;
import consulo.execution.ui.RunContentDescriptor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import consulo.util.concurrent.Promises;

public abstract class RsDefaultProgramRunnerBase implements ProgramRunner<RunnerSettings> {

    @Override
    public void execute(@Nonnull ExecutionEnvironment environment) throws ExecutionException {
        RunProfileState state = environment.getState();
        if (state == null) return;
        @SuppressWarnings("UnstableApiUsage")
        ExecutionManager executionManager = ExecutionManager.getInstance(environment.getProject());
        executionManager.startRunProfile(
            (runState, runEnv) -> {
                try {
                    return java.util.concurrent.CompletableFuture.completedFuture(doExecute(runState, runEnv));
                } catch (ExecutionException e) {
                    return java.util.concurrent.CompletableFuture.failedFuture(e);
                }
            },
            state,
            environment
        );
    }

    @Nullable
    protected RunContentDescriptor doExecute(@Nonnull RunProfileState state, @Nonnull ExecutionEnvironment environment) throws ExecutionException {
        consulo.execution.ExecutionResult executionResult = state.execute(environment.getExecutor(), this);
        if (executionResult == null) return null;
        return new RunContentBuilder(executionResult, environment).showRunContent(environment.getContentToReuse());
    }
}
