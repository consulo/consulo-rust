/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.console;

import com.intellij.execution.filters.TextConsoleBuilderImpl;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.ExecutionSearchScopes;
import com.intellij.terminal.TerminalExecutionConsole;
import org.jetbrains.annotations.NotNull;
import org.rust.cargo.runconfig.RsCommandConfiguration;
import org.rust.cargo.runconfig.RunConfigUtil;

public class RsConsoleBuilder extends TextConsoleBuilderImpl {

    private final RsCommandConfiguration myConfig;

    public RsConsoleBuilder(@NotNull Project project, @NotNull RsCommandConfiguration config) {
        super(project, ExecutionSearchScopes.executionScope(project, config));
        myConfig = config;
    }

    @NotNull
    public RsCommandConfiguration getConfig() {
        return myConfig;
    }

    @NotNull
    @Override
    protected ConsoleView createConsole() {
        if (myConfig.getEmulateTerminal() && !RunConfigUtil.hasRemoteTarget(myConfig)) {
            return new TerminalExecutionConsole(getProject(), null);
        } else {
            return new CargoConsoleView(getProject(), getScope(), isViewer(), true);
        }
    }
}
