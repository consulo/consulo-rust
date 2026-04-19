/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.buildtool;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.Key;
import com.intellij.task.ProjectTaskManager;
import org.rust.RsBundle;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;

import javax.swing.*;
import java.util.concurrent.CompletableFuture;

public abstract class RsBuildTaskProvider<T extends RsBuildTaskProvider.BuildTask<T>> extends BeforeRunTaskProvider<T> {

    @Override
    public String getName() {
        return RsBundle.message("build");
    }

    @Override
    public Icon getIcon() {
        return AllIcons.Actions.Compile;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    protected boolean doExecuteTask(CargoCommandConfiguration buildConfiguration, ExecutionEnvironment environment) {
        ExecutionEnvironment buildEnvironment = CargoBuildManager.INSTANCE.createBuildEnvironment(buildConfiguration, environment);
        if (buildEnvironment == null) return false;
        CargoBuildConfiguration buildableElement = new CargoBuildConfiguration(buildConfiguration, buildEnvironment);

        CompletableFuture<Boolean> result = new CompletableFuture<>();
        ProjectTaskManager.getInstance(environment.getProject()).build(buildableElement).onProcessed(taskResult ->
            result.complete(!taskResult.hasErrors() && !taskResult.isAborted())
        );
        try {
            return result.get();
        } catch (Exception e) {
            return false;
        }
    }

    public abstract static class BuildTask<T extends BuildTask<T>> extends BeforeRunTask<T> {
        public BuildTask(Key<T> providerId) {
            super(providerId);
            setEnabled(true);
        }
    }
}
