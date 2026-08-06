/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.console;

import consulo.ide.impl.idea.execution.filters.TextConsoleBuilderImpl;
import consulo.execution.ui.console.ConsoleView;
import consulo.project.Project;
import consulo.language.psi.scope.GlobalSearchScope;
import jakarta.annotation.Nonnull;
import org.rust.cargo.runconfig.RsCommandConfiguration;
import org.rust.cargo.runconfig.RunConfigUtil;

public class RsConsoleBuilder extends TextConsoleBuilderImpl {

    private final RsCommandConfiguration myConfig;

    public RsConsoleBuilder(@Nonnull Project project, @Nonnull RsCommandConfiguration config) {
        super(project, GlobalSearchScope.allScope(project));
        myConfig = config;
    }

    @Nonnull
    public RsCommandConfiguration getConfig() {
        return myConfig;
    }

    @Nonnull
    @Override
    protected ConsoleView createConsole() {
        // TerminalExecutionConsole is IntelliJ-only; always use the Cargo console view
        return new CargoConsoleView(getProject(), (GlobalSearchScope) getScope(), isViewer(), true);
    }
}
