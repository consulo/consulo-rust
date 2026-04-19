/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.settings;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.BaseState;
import com.intellij.openapi.components.SimplePersistentStateComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class RsProjectSettingsServiceBase<T extends RsProjectSettingsServiceBase.RsProjectSettingsBase<T>>
    extends SimplePersistentStateComponent<T> {

    @NotNull
    public final Project project;

    protected RsProjectSettingsServiceBase(@NotNull Project project, @NotNull T state) {
        super(state);
        this.project = project;
    }

    public abstract static class RsProjectSettingsBase<T extends RsProjectSettingsBase<T>> extends BaseState {
        public abstract T copy();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    protected @interface AffectsCargoMetadata {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    protected @interface AffectsHighlighting {
    }

    public void modify(@NotNull Consumer<T> action) {
        T oldState = getState().copy();
        action.accept(getState());
        T newState = getState();
        SettingsChangedEventBase<T> event = createSettingsChangedEvent(oldState, newState);
        notifySettingsChanged(event);
    }

    @TestOnly
    public void modifyTemporary(@NotNull Disposable parentDisposable, @NotNull Consumer<T> action) {
        T oldState = getState();
        T newState = oldState.copy();
        action.accept(newState);
        loadState(newState);
        Disposer.register(parentDisposable, () -> loadState(oldState));
    }

    public static final Topic<RsSettingsListener> RUST_SETTINGS_TOPIC = Topic.create(
        "rust settings changes",
        RsSettingsListener.class,
        Topic.BroadcastDirection.TO_PARENT
    );

    public interface RsSettingsListener {
        <T extends RsProjectSettingsBase<T>> void settingsChanged(@NotNull SettingsChangedEventBase<T> e);
    }

    @NotNull
    protected abstract SettingsChangedEventBase<T> createSettingsChangedEvent(@NotNull T oldEvent, @NotNull T newEvent);

    protected void notifySettingsChanged(@NotNull SettingsChangedEventBase<T> event) {
        project.getMessageBus().syncPublisher(RUST_SETTINGS_TOPIC).settingsChanged(event);

        if (event.getAffectsHighlighting()) {
            DaemonCodeAnalyzer.getInstance(project).restart();
        }
    }

    public abstract static class SettingsChangedEventBase<T extends RsProjectSettingsBase<T>> {
        @NotNull
        private final T oldState;
        @NotNull
        private final T newState;

        protected SettingsChangedEventBase(@NotNull T oldState, @NotNull T newState) {
            this.oldState = oldState;
            this.newState = newState;
        }

        @NotNull
        public T getOldState() {
            return oldState;
        }

        @NotNull
        public T getNewState() {
            return newState;
        }

        public boolean getAffectsCargoMetadata() {
            for (Field field : oldState.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(AffectsCargoMetadata.class)) {
                    field.setAccessible(true);
                    try {
                        if (!java.util.Objects.equals(field.get(oldState), field.get(newState))) {
                            return true;
                        }
                    } catch (IllegalAccessException ignored) {
                    }
                }
            }
            return false;
        }

        public boolean getAffectsHighlighting() {
            for (Field field : oldState.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(AffectsHighlighting.class)) {
                    field.setAccessible(true);
                    try {
                        if (!java.util.Objects.equals(field.get(oldState), field.get(newState))) {
                            return true;
                        }
                    } catch (IllegalAccessException ignored) {
                    }
                }
            }
            return false;
        }
    }
}
