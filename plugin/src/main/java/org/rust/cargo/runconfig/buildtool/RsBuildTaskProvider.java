/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.buildtool;

import consulo.application.AllIcons;
import consulo.execution.BeforeRunTask;
import consulo.execution.BeforeRunTaskProvider;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import com.intellij.task.ProjectTaskContext;
import com.intellij.task.impl.ProjectModelBuildTaskImpl;
import org.rust.RsBundle;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;

import java.util.concurrent.CompletableFuture;

public abstract class RsBuildTaskProvider<T extends RsBuildTaskProvider.BuildTask<T>> extends BeforeRunTaskProvider<T> {

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.of(RsBundle.message("build"));
    }

    @Override
    public Image getIcon(RunConfiguration runConfiguration) {
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
        new CargoBuildTaskRunner()
            .run(environment.getProject(),
                new ProjectTaskContext(),
                new ProjectModelBuildTaskImpl<>(buildableElement, true))
            .onProcessed(taskResult ->
                result.complete(taskResult != null && !taskResult.hasErrors() && !taskResult.isAborted())
            )
            .onError(error -> result.complete(false));
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
