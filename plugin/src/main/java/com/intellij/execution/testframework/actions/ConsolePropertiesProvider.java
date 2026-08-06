package com.intellij.execution.testframework.actions;
import consulo.execution.test.sm.runner.SMTRunnerConsoleProperties;
import consulo.execution.executor.Executor;
public interface ConsolePropertiesProvider {
    SMTRunnerConsoleProperties createTestConsoleProperties(Executor executor);
}
