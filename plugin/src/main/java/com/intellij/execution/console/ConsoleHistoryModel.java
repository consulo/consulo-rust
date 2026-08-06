package com.intellij.execution.console;
/** IntelliJ-compat stub. */
public interface ConsoleHistoryModel {
    int getHistorySize();
    void addToHistory(String statement);
    String getHistoryNext();
    String getHistoryPrev();
}
