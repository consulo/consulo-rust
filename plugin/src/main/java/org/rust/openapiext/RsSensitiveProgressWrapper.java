/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressIndicatorListener;
import consulo.application.progress.StandardProgressIndicator;
import consulo.application.progress.WrappedProgressIndicator;
import consulo.component.ProcessCanceledException;
import consulo.localize.LocalizeValue;
import consulo.ui.ModalityState;
import jakarta.annotation.Nonnull;

/**
 * A progress indicator that can be cancelled independently of the one it wraps, while still
 * reporting cancellation of that original.
 * <p>
 * Replaces {@code consulo.application.internal.SensitiveProgressWrapper}. The public
 * {@link consulo.application.progress.DelegatingProgressIndicator} is not a substitute: its
 * {@code cancel()} and {@code isCanceled()} are {@code final} and delegate straight to the wrapped
 * indicator, so cancelling the wrapper would cancel the whole parent progress. The point of this
 * class is the opposite — a retry loop cancels the wrapper when a write action wants the lock, and
 * the parent progress must survive to be retried.
 */
public class RsSensitiveProgressWrapper implements WrappedProgressIndicator, StandardProgressIndicator {

    @Nonnull
    private final ProgressIndicator myOriginal;

    private volatile boolean myCanceled;

    private volatile boolean myRunning;

    public RsSensitiveProgressWrapper(@Nonnull ProgressIndicator original) {
        myOriginal = original;
    }

    @Nonnull
    @Override
    public ProgressIndicator getOriginalProgressIndicator() {
        return myOriginal;
    }

    /** Cancels only this wrapper — the wrapped indicator keeps running. */
    @Override
    public void cancel() {
        myCanceled = true;
    }

    @Override
    public boolean isCanceled() {
        return myCanceled || myOriginal.isCanceled();
    }

    @Override
    public void checkCanceled() throws ProcessCanceledException {
        if (isCanceled()) {
            throw new ProcessCanceledException();
        }
    }

    /**
     * The running state is the wrapper's own. The wrapped indicator is already running by the time it
     * is wrapped, and starting it a second time is an error once it has been cancelled.
     */
    @Override
    public void start() {
        myRunning = true;
    }

    @Override
    public void stop() {
        myRunning = false;
    }

    @Override
    public boolean isRunning() {
        return myRunning;
    }

    // Everything below is plain delegation.

    @Override
    public void setText(LocalizeValue text) {
        myOriginal.setText(text);
    }

    @Override
    public LocalizeValue getText() {
        return myOriginal.getText();
    }

    @Override
    public void setText2(LocalizeValue text) {
        myOriginal.setText2(text);
    }

    @Override
    public LocalizeValue getText2() {
        return myOriginal.getText2();
    }

    @Override
    public double getFraction() {
        return myOriginal.getFraction();
    }

    @Override
    public void setFraction(double fraction) {
        myOriginal.setFraction(fraction);
    }

    @Override
    public void pushState() {
        myOriginal.pushState();
    }

    @Override
    public void popState() {
        myOriginal.popState();
    }

    @Override
    public boolean isModal() {
        return myOriginal.isModal();
    }

    @Nonnull
    @Override
    public ModalityState getModalityState() {
        return myOriginal.getModalityState();
    }

    @Override
    public void setModalityProgress(ProgressIndicator modalityProgress) {
        myOriginal.setModalityProgress(modalityProgress);
    }

    @Override
    public boolean isIndeterminate() {
        return myOriginal.isIndeterminate();
    }

    @Override
    public void setIndeterminate(boolean indeterminate) {
        myOriginal.setIndeterminate(indeterminate);
    }

    @Override
    public boolean isPopupWasShown() {
        return myOriginal.isPopupWasShown();
    }

    @Override
    public boolean isShowing() {
        return myOriginal.isShowing();
    }

    @Override
    public void addListener(ProgressIndicatorListener listener) {
        myOriginal.addListener(listener);
    }
}
