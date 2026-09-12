package com.intellij.task;
public final class TaskRunnerResults {
    public static final ProjectTaskRunner.Result SUCCESS = new ProjectTaskRunner.Result() {
        @Override public boolean isAborted() { return false; }
        @Override public boolean hasErrors() { return false; }
    };
    public static final ProjectTaskRunner.Result FAILURE = new ProjectTaskRunner.Result() {
        @Override public boolean isAborted() { return false; }
        @Override public boolean hasErrors() { return true; }
    };
    public static final ProjectTaskRunner.Result ABORTED = new ProjectTaskRunner.Result() {
        @Override public boolean isAborted() { return true; }
        @Override public boolean hasErrors() { return false; }
    };
}
