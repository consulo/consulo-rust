package com.intellij.ide;
public interface CommandLineInspectionProgressReporter {
    void reportError(String message);
    void reportMessage(int minVerboseLevel, String message);
}
