/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.runners.RunContentBuilder;
import com.intellij.execution.ui.RunContentDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.Promises;

public abstract class RsDefaultProgramRunnerBase implements ProgramRunner<RunnerSettings> {

    @Override
    public void execute(@NotNull ExecutionEnvironment environment) throws ExecutionException {
        RunProfileState state = environment.getState();
        if (state == null) return;
        @SuppressWarnings("UnstableApiUsage")
        ExecutionManager executionManager = ExecutionManager.getInstance(environment.getProject());
        executionManager.startRunProfile(environment, () -> {
            try {
                return Promises.resolvedPromise(doExecute(state, environment));
            } catch (ExecutionException e) {
                return Promises.rejectedPromise(e);
            }
        });
    }

    @Nullable
    protected RunContentDescriptor doExecute(@NotNull RunProfileState state, @NotNull ExecutionEnvironment environment) throws ExecutionException {
        com.intellij.execution.ExecutionResult executionResult = state.execute(environment.getExecutor(), this);
        if (executionResult == null) return null;
        return new RunContentBuilder(executionResult, environment).showRunContent(environment.getContentToReuse());
    }
}
