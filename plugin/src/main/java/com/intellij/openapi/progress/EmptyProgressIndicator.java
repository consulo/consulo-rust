package com.intellij.openapi.progress;
import consulo.application.progress.ProgressIndicator;
/** IntelliJ-compat stub. Minimal no-op progress indicator. */
public class EmptyProgressIndicator implements ProgressIndicator {
    private volatile boolean canceled = false;
    private volatile boolean running = false;
    private volatile boolean indeterminate = false;
    private volatile double fraction = 0;
    @Override public void start() { running = true; }
    @Override public void stop() { running = false; }
    @Override public boolean isRunning() { return running; }
    @Override public void cancel() { canceled = true; }
    @Override public boolean isCanceled() { return canceled; }
    @Override public void setText(String text) {}
    @Override public void setText(consulo.localize.LocalizeValue text) {}
    @Override public consulo.localize.LocalizeValue getText() { return consulo.localize.LocalizeValue.empty(); }
    @Override public void setText2(String text) {}
    @Override public void setText2(consulo.localize.LocalizeValue text) {}
    @Override public consulo.localize.LocalizeValue getText2() { return consulo.localize.LocalizeValue.empty(); }
    @Override public double getFraction() { return fraction; }
    @Override public void setFraction(double fraction) { this.fraction = fraction; }
    @Override public void pushState() {}
    @Override public void popState() {}
    @Override public boolean isModal() { return false; }
    @Override public consulo.ui.ModalityState getModalityState() { return consulo.ui.ModalityState.nonModal(); }
    @Override public void setModalityProgress(consulo.application.progress.ProgressIndicator modalityProgress) {}
    @Override public boolean isIndeterminate() { return indeterminate; }
    @Override public void setIndeterminate(boolean indeterminate) { this.indeterminate = indeterminate; }
    @Override public void checkCanceled() throws consulo.component.ProcessCanceledException { if (canceled) throw new consulo.component.ProcessCanceledException(); }
    @Override public boolean isPopupWasShown() { return false; }
    @Override public boolean isShowing() { return false; }
}
