/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.test;

import consulo.execution.executor.Executor;
import consulo.execution.configuration.RunConfiguration;
import consulo.execution.test.Printer;
import consulo.execution.test.TestConsoleProperties;
import consulo.execution.test.sm.SMCustomMessagesParsing;
import consulo.execution.test.sm.runner.OutputToGeneralTestEventsConverter;
import consulo.execution.test.sm.runner.SMTRunnerConsoleProperties;
import consulo.execution.test.sm.runner.SMTestLocator;
import consulo.execution.ui.console.ConsoleViewContentType;
import consulo.util.lang.SemVer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class CargoTestConsoleProperties extends SMTRunnerConsoleProperties implements SMCustomMessagesParsing {

    public static final String TEST_FRAMEWORK_NAME = "Cargo Test";
    public static final String TEST_TOOL_WINDOW_SETTING_KEY = "org.rust.cargo.test.tool.window";
    public static final boolean TEST_TOOL_WINDOW_DEFAULT = true;

    @Nullable
    private final SemVer rustcVersion;

    public CargoTestConsoleProperties(
        @Nonnull RunConfiguration config,
        @Nonnull Executor executor,
        @Nullable SemVer rustcVersion
    ) {
        super(config, TEST_FRAMEWORK_NAME, executor);
        this.rustcVersion = rustcVersion;
        setIdBasedTestTree(true);
    }

    @Nonnull
    @Override
    public SMTestLocator getTestLocator() {
        return CargoTestLocator.INSTANCE;
    }

    @Nonnull
    @Override
    public OutputToGeneralTestEventsConverter createTestEventsConverter(
        @Nonnull String testFrameworkName,
        @Nonnull TestConsoleProperties consoleProperties
    ) {
        return new CargoTestEventsConverter(testFrameworkName, consoleProperties, rustcVersion);
    }

    public void printExpectedActualHeader(@Nonnull Printer printer, @Nonnull String expected, @Nonnull String actual) {
        printer.print("\n", ConsoleViewContentType.ERROR_OUTPUT);
        printer.print("Left:  ", ConsoleViewContentType.SYSTEM_OUTPUT);
        printer.print(actual + "\n", ConsoleViewContentType.ERROR_OUTPUT);
        printer.print("Right: ", ConsoleViewContentType.SYSTEM_OUTPUT);
        printer.print(expected, ConsoleViewContentType.ERROR_OUTPUT);
    }
}
