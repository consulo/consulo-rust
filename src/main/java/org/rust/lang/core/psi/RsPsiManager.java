/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.util.SimpleModificationTracker;
import com.intellij.psi.PsiFile;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface RsPsiManager {
    @NotNull
    ModificationTracker getRustStructureModificationTracker();

    @NotNull
    SimpleModificationTracker getRustStructureModificationTrackerInDependencies();

    void incRustStructureModificationCount();

    default void subscribeRustStructureChange(@NotNull MessageBusConnection connection,
                                               @NotNull RustStructureChangeListener listener) {
        connection.subscribe(RsPsiManagerUtil.getRUST_STRUCTURE_CHANGE_TOPIC(), listener);
    }

    default void subscribeRustPsiChange(@NotNull MessageBusConnection connection,
                                         @NotNull RustPsiChangeListener listener) {
        connection.subscribe(RsPsiManagerUtil.getRUST_PSI_CHANGE_TOPIC(), listener);
    }

    Key<Boolean> IGNORE_PSI_EVENTS = Key.create("IGNORE_PSI_EVENTS");

    static <T> T withIgnoredPsiEvents(@NotNull PsiFile psi, @NotNull java.util.function.Supplier<T> f) {
        setIgnorePsiEvents(psi, true);
        try {
            return f.get();
        } finally {
            setIgnorePsiEvents(psi, false);
        }
    }

    static void withIgnoredPsiEvents(@NotNull PsiFile psi, @NotNull Runnable f) {
        setIgnorePsiEvents(psi, true);
        try {
            f.run();
        } finally {
            setIgnorePsiEvents(psi, false);
        }
    }

    static boolean isIgnorePsiEvents(@NotNull PsiFile psi) {
        return Boolean.TRUE.equals(psi.getUserData(IGNORE_PSI_EVENTS));
    }

    static void setIgnorePsiEvents(@NotNull PsiFile psi, boolean ignore) {
        psi.putUserData(IGNORE_PSI_EVENTS, ignore ? Boolean.TRUE : null);
    }
}
