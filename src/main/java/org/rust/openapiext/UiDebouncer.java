/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.Alarm;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class UiDebouncer {
    private final Disposable parentDisposable;
    private final int delayMillis;
    private final Alarm alarm;

    public UiDebouncer(@NotNull Disposable parentDisposable) {
        this(parentDisposable, 200);
    }

    public UiDebouncer(@NotNull Disposable parentDisposable, int delayMillis) {
        this.parentDisposable = parentDisposable;
        this.delayMillis = delayMillis;
        this.alarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, parentDisposable);
    }

    /**
     * @param onUiThread callback to be executed in EDT with <b>any</b> modality state.
     *                   Use it only for UI updates.
     */
    public <T> void run(@NotNull Supplier<T> onPooledThread, @NotNull Consumer<T> onUiThread) {
        if (Disposer.isDisposed(parentDisposable)) return;
        alarm.cancelAllRequests();
        alarm.addRequest(() -> {
            T r = onPooledThread.get();
            ApplicationManager.getApplication().invokeLater(() -> {
                if (!Disposer.isDisposed(parentDisposable)) {
                    onUiThread.accept(r);
                }
            }, ModalityState.any());
        }, delayMillis);
    }
}
