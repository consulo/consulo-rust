/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.console;

import com.intellij.execution.Executor;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil;
import com.intellij.execution.ui.ConsoleView;
import org.jetbrains.annotations.NotNull;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.runconfig.test.CargoTestConsoleProperties;

import java.util.ArrayList;
import java.util.List;

public class CargoTestConsoleBuilder extends TextConsoleBuilder {

    private final CargoCommandConfiguration myConfig;
    private final Executor myExecutor;
    private final List<Filter> myFilters = new ArrayList<>();

    public CargoTestConsoleBuilder(@NotNull CargoCommandConfiguration config, @NotNull Executor executor) {
        myConfig = config;
        myExecutor = executor;
    }

    @Override
    public void addFilter(@NotNull Filter filter) {
        myFilters.add(filter);
    }

    @Override
    public void setViewer(boolean isViewer) {
    }

    @NotNull
    @Override
    public ConsoleView getConsole() {
        Object consoleProperties = myConfig.createTestConsoleProperties(myExecutor);
        ConsoleView consoleView = SMTestRunnerConnectionUtil.createConsole(
            CargoTestConsoleProperties.TEST_FRAMEWORK_NAME,
            (com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties) consoleProperties
        );
        for (Filter filter : myFilters) {
            consoleView.addMessageFilter(filter);
        }
        return consoleView;
    }
}
