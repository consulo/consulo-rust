package com.intellij.openapi.ui;
/** IntelliJ-compat stub. */
public interface DoNotAskOption {
    boolean isToBeShown();
    void setToBeShown(boolean toBeShown, int exitCode);
    boolean canBeHidden();
    boolean shouldSaveOptionsOnCancel();
    String getDoNotShowMessage();

    abstract class Adapter implements DoNotAskOption {
        @Override public boolean isToBeShown() { return true; }
        @Override public final void setToBeShown(boolean toBeShown, int exitCode) { rememberChoice(!toBeShown, exitCode); }
        @Override public boolean canBeHidden() { return true; }
        @Override public boolean shouldSaveOptionsOnCancel() { return true; }
        @Override public String getDoNotShowMessage() { return "Don't show again"; }
        public abstract void rememberChoice(boolean isSelected, int exitCode);
    }
}
