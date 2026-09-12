/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.console;

import consulo.execution.executor.Executor;
import consulo.execution.ui.console.Filter;
import consulo.execution.ui.console.TextConsoleBuilder;
import consulo.execution.test.sm.SMTestRunnerConnectionUtil;
import consulo.execution.ui.console.ConsoleView;
import jakarta.annotation.Nonnull;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.runconfig.test.CargoTestConsoleProperties;

import java.util.ArrayList;
import java.util.List;
import consulo.execution.test.sm.runner.SMTRunnerConsoleProperties;
import consulo.execution.ui.console.ConsoleState;

public class CargoTestConsoleBuilder extends TextConsoleBuilder {

    private final CargoCommandConfiguration myConfig;
    private final Executor myExecutor;
    private final List<Filter> myFilters = new ArrayList<>();

    public CargoTestConsoleBuilder(@Nonnull CargoCommandConfiguration config, @Nonnull Executor executor) {
        myConfig = config;
        myExecutor = executor;
    }

    @Override
    public void addFilter(@Nonnull Filter filter) {
        myFilters.add(filter);
    }

    @Override
    public void setViewer(boolean isViewer) {
    }

    @Override
    public void setState(@Nonnull consulo.execution.ui.console.ConsoleState state) {
    }

    @Override
    public void setUsePredefinedMessageFilter(boolean usePredefinedMessageFilter) {
    }

    @Nonnull
    @Override
    public ConsoleView getConsole() {
        Object consoleProperties = myConfig.createTestConsoleProperties(myExecutor);
        ConsoleView consoleView = SMTestRunnerConnectionUtil.createConsole(
            CargoTestConsoleProperties.TEST_FRAMEWORK_NAME,
            (consulo.execution.test.sm.runner.SMTRunnerConsoleProperties) consoleProperties
        );
        for (Filter filter : myFilters) {
            consoleView.addMessageFilter(filter);
        }
        return consoleView;
    }
}
