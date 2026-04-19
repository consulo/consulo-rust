/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.test;

import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.testframework.Printer;
import com.intellij.execution.testframework.TestConsoleProperties;
import com.intellij.execution.testframework.sm.SMCustomMessagesParsing;
import com.intellij.execution.testframework.sm.runner.OutputToGeneralTestEventsConverter;
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties;
import com.intellij.execution.testframework.sm.runner.SMTestLocator;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.util.text.SemVer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CargoTestConsoleProperties extends SMTRunnerConsoleProperties implements SMCustomMessagesParsing {

    public static final String TEST_FRAMEWORK_NAME = "Cargo Test";
    public static final String TEST_TOOL_WINDOW_SETTING_KEY = "org.rust.cargo.test.tool.window";

    @Nullable
    private final SemVer rustcVersion;

    public CargoTestConsoleProperties(
        @NotNull RunConfiguration config,
        @NotNull Executor executor,
        @Nullable SemVer rustcVersion
    ) {
        super(config, TEST_FRAMEWORK_NAME, executor);
        this.rustcVersion = rustcVersion;
        setIdBasedTestTree(true);
    }

    @NotNull
    @Override
    public SMTestLocator getTestLocator() {
        return CargoTestLocator.INSTANCE;
    }

    @NotNull
    @Override
    public OutputToGeneralTestEventsConverter createTestEventsConverter(
        @NotNull String testFrameworkName,
        @NotNull TestConsoleProperties consoleProperties
    ) {
        return new CargoTestEventsConverter(testFrameworkName, consoleProperties, rustcVersion);
    }

    @Override
    public void printExpectedActualHeader(@NotNull Printer printer, @NotNull String expected, @NotNull String actual) {
        printer.print("\n", ConsoleViewContentType.ERROR_OUTPUT);
        printer.print("Left:  ", ConsoleViewContentType.SYSTEM_OUTPUT);
        printer.print(actual + "\n", ConsoleViewContentType.ERROR_OUTPUT);
        printer.print("Right: ", ConsoleViewContentType.SYSTEM_OUTPUT);
        printer.print(expected, ConsoleViewContentType.ERROR_OUTPUT);
    }
}
