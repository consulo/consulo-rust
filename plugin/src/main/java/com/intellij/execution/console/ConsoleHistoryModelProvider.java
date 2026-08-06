package com.intellij.execution.console;
import consulo.codeEditor.Editor;
/** IntelliJ-compat stub. */
public interface ConsoleHistoryModelProvider {
    ConsoleHistoryModel createModel(String persistenceId, Editor editor);
}
