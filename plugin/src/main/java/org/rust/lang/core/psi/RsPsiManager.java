/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import consulo.util.dataholder.Key;
import consulo.component.util.ModificationTracker;
import consulo.component.util.SimpleModificationTracker;
import consulo.language.psi.PsiFile;
import consulo.component.messagebus.MessageBusConnection;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ComponentScope;

@ServiceAPI(ComponentScope.PROJECT)
public interface RsPsiManager {
    @Nonnull
    ModificationTracker getRustStructureModificationTracker();

    @Nonnull
    SimpleModificationTracker getRustStructureModificationTrackerInDependencies();

    void incRustStructureModificationCount();

    default void subscribeRustStructureChange(@Nonnull MessageBusConnection connection,
                                               @Nonnull RustStructureChangeListener listener) {
        connection.subscribe(RsPsiManagerUtil.getRUST_STRUCTURE_CHANGE_TOPIC(), listener);
    }

    default void subscribeRustPsiChange(@Nonnull MessageBusConnection connection,
                                         @Nonnull RustPsiChangeListener listener) {
        connection.subscribe(RsPsiManagerUtil.getRUST_PSI_CHANGE_TOPIC(), listener);
    }

    Key<Boolean> IGNORE_PSI_EVENTS = Key.create("IGNORE_PSI_EVENTS");

    static <T> T withIgnoredPsiEvents(@Nonnull PsiFile psi, @Nonnull java.util.function.Supplier<T> f) {
        setIgnorePsiEvents(psi, true);
        try {
            return f.get();
        } finally {
            setIgnorePsiEvents(psi, false);
        }
    }

    static void withIgnoredPsiEvents(@Nonnull PsiFile psi, @Nonnull Runnable f) {
        setIgnorePsiEvents(psi, true);
        try {
            f.run();
        } finally {
            setIgnorePsiEvents(psi, false);
        }
    }

    static boolean isIgnorePsiEvents(@Nonnull PsiFile psi) {
        return Boolean.TRUE.equals(psi.getUserData(IGNORE_PSI_EVENTS));
    }

    static void setIgnorePsiEvents(@Nonnull PsiFile psi, boolean ignore) {
        psi.putUserData(IGNORE_PSI_EVENTS, ignore ? Boolean.TRUE : null);
    }
}
