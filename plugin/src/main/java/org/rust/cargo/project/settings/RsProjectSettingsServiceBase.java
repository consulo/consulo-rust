/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.settings;

import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.disposer.Disposable;
import com.intellij.openapi.components.BaseState;
import com.intellij.openapi.components.SimplePersistentStateComponent;
import consulo.project.Project;
import consulo.disposer.Disposer;
import jakarta.annotation.Nonnull;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;
import consulo.annotation.component.TopicBroadcastDirection;

public abstract class RsProjectSettingsServiceBase<T extends RsProjectSettingsServiceBase.RsProjectSettingsBase<T>>
    extends SimplePersistentStateComponent<T> {

    @Nonnull
    public final Project project;

    protected RsProjectSettingsServiceBase(@Nonnull Project project, @Nonnull T state) {
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

    public void modify(@Nonnull Consumer<T> action) {
        T oldState = getState().copy();
        action.accept(getState());
        T newState = getState();
        SettingsChangedEventBase<T> event = createSettingsChangedEvent(oldState, newState);
        notifySettingsChanged(event);
    }

    
    public void modifyTemporary(@Nonnull Disposable parentDisposable, @Nonnull Consumer<T> action) {
        T oldState = getState();
        T newState = oldState.copy();
        action.accept(newState);
        loadState(newState);
        Disposer.register(parentDisposable, () -> loadState(oldState));
    }

    public static final Class<RsSettingsListener> RUST_SETTINGS_TOPIC = RsSettingsListener.class;


    @Nonnull
    protected abstract SettingsChangedEventBase<T> createSettingsChangedEvent(@Nonnull T oldEvent, @Nonnull T newEvent);

    protected void notifySettingsChanged(@Nonnull SettingsChangedEventBase<T> event) {
        project.getMessageBus().syncPublisher(RUST_SETTINGS_TOPIC).settingsChanged(event);

        if (event.getAffectsHighlighting()) {
            DaemonCodeAnalyzer.getInstance(project).restart();
        }
    }

    public abstract static class SettingsChangedEventBase<T extends RsProjectSettingsBase<T>> {
        @Nonnull
        private final T oldState;
        @Nonnull
        private final T newState;

        protected SettingsChangedEventBase(@Nonnull T oldState, @Nonnull T newState) {
            this.oldState = oldState;
            this.newState = newState;
        }

        @Nonnull
        public T getOldState() {
            return oldState;
        }

        @Nonnull
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
