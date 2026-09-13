/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions.runAnything;

import consulo.execution.executor.Executor;
import consulo.execution.executor.DefaultRunExecutor;
import consulo.ide.impl.idea.ide.actions.runAnything.RunAnythingAction;
import consulo.ide.runAnything.RunAnythingContext;
import consulo.ide.impl.idea.ide.actions.runAnything.RunAnythingUtil;
import consulo.ide.runAnything.RunAnythingProviderBase;
import consulo.ide.runAnything.RunAnythingItem;
import consulo.language.editor.CommonDataKeys;
import consulo.dataContext.DataContext;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import consulo.process.cmd.ParametersListUtil;
import org.rust.cargo.api.model.CargoProject;
import org.rust.ide.cargo.completion.RsCommandCompletionProvider;
import org.rust.stdext.Utils;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.rust.cargo.runconfig.RunConfigUtil;

public abstract class RsRunAnythingProvider extends RunAnythingProviderBase<String> {

    public abstract RunAnythingItem getMainListItem(DataContext dataContext, String value);

    protected abstract void run(
        Executor executor,
        String command,
        List<String> params,
        Path workingDirectory,
        CargoProject cargoProject
    );

    public abstract RsCommandCompletionProvider getCompletionProvider(Project project, DataContext dataContext);

    @Override
    public String findMatchingValue(DataContext dataContext, String pattern) {
        return pattern.startsWith(getHelpCommand()) ? getCommand(pattern) : null;
    }

    @Override
    public Collection<String> getValues(DataContext dataContext, String pattern) {
        Project project = dataContext.getData(CommonDataKeys.PROJECT);
        if (project == null) return Collections.emptyList();
        if (!org.rust.cargo.runconfig.RunConfigUtil.hasCargoProject(project)) return Collections.emptyList();
        RsCommandCompletionProvider completionProvider = getCompletionProvider(project, dataContext);

        if (pattern.startsWith(getHelpCommand())) {
            String context = StringUtil.trimStart(pattern, getHelpCommand()).replaceAll("\\s+\\S*$", "");
            String prefix = pattern.replaceAll("\\s+\\S*$", "");
            return completionProvider.complete(context).stream()
                .map(item -> prefix + " " + item.getLookupString())
                .collect(Collectors.toList());
        } else if (!pattern.isBlank() && getHelpCommand().startsWith(pattern)) {
            return completionProvider.complete("").stream()
                .map(item -> getHelpCommand() + " " + item.getLookupString())
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    public void execute(DataContext dataContext, String value) {
        Project project = dataContext.getData(CommonDataKeys.PROJECT);
        if (project == null) return;
        if (!org.rust.cargo.runconfig.RunConfigUtil.hasCargoProject(project)) return;
        CargoProject cargoProject = org.rust.cargo.runconfig.RunConfigUtil.getAppropriateCargoProject(dataContext);
        if (cargoProject == null) return;
        List<String> params = ParametersListUtil.parse(StringUtil.trimStart(value, getHelpCommand()));
        RunAnythingContext executionContext = dataContext.getData(EXECUTING_CONTEXT);
        if (executionContext == null) executionContext = new RunAnythingContext.ProjectContext(project);
        String pathStr = executionContext instanceof RunAnythingContext.ProjectContext
            ? ((RunAnythingContext.ProjectContext) executionContext).getProject().getBasePath()
            : null;
        if (pathStr == null) return;
        Path path = java.nio.file.Paths.get(pathStr);
        Executor executor = dataContext.getData(RunAnythingAction.EXECUTOR_KEY);
        if (executor == null) executor = DefaultRunExecutor.getRunExecutorInstance();
        String command = params.isEmpty() ? "--help" : params.get(0);
        List<String> restParams = params.size() > 1 ? params.subList(1, params.size()) : Collections.emptyList();
        run(executor, command, restParams, path, cargoProject);
    }

    public abstract String getHelpCommand();
}
