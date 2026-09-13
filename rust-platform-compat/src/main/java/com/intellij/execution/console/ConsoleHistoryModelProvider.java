package com.intellij.execution.console;
import consulo.codeEditor.Editor;
/** Supplies the command history model for a console editor. */
public interface ConsoleHistoryModelProvider {
    ConsoleHistoryModel createModel(String persistenceId, Editor editor);
}
