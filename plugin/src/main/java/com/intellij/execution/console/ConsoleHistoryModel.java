package com.intellij.execution.console;
public interface ConsoleHistoryModel {
    int getHistorySize();
    void addToHistory(String statement);
    String getHistoryNext();
    String getHistoryPrev();
}
