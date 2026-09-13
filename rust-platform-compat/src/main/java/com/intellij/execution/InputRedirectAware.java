package com.intellij.execution;
public interface InputRedirectAware {
    InputRedirectOptions getInputRedirectOptions();
    interface InputRedirectOptions {
        boolean isRedirectInput();
        String getRedirectInputPath();
    }
}
