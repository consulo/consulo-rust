/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.notifications;

import consulo.dataContext.DataContext;
import consulo.fileEditor.EditorNotificationBuilder;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnActionEvent;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ui.Component;
import consulo.project.ui.notification.NotificationType;
import consulo.ui.event.ComponentEvent;
import consulo.ui.event.ComponentEventListener;
import jakarta.annotation.Nonnull;

/**
 * Thin wrapper around {@link EditorNotificationBuilder} that keeps the panel-flavoured API used by the
 * Rust editor notification providers.
 */
public class RsEditorNotificationPanel {

    public static final String NOTIFICATION_PANEL_PLACE = "RsEditorNotificationPanel";

    private final String myDebugId;
    private final EditorNotificationBuilder myBuilder;

    public RsEditorNotificationPanel(@Nonnull String debugId, @Nonnull EditorNotificationBuilder builder) {
        myDebugId = debugId;
        myBuilder = builder;
    }

    @Nonnull
    public String getDebugId() {
        return myDebugId;
    }

    @Nonnull
    public EditorNotificationBuilder getBuilder() {
        return myBuilder;
    }

    public void setText(@Nonnull String text) {
        myBuilder.withText(LocalizeValue.of(text));
    }

    public void setType(@Nonnull NotificationType type) {
        myBuilder.withType(type.toUI());
    }

    /** Adds a clickable label running {@code action} on click. */
    public void createActionLabel(@Nonnull String text, @Nonnull Runnable action) {
        ComponentEventListener<Component, ComponentEvent<Component>> listener = event -> action.run();
        myBuilder.withAction(LocalizeValue.of(text), listener);
    }

    /** Adds a clickable label triggering the action registered under {@code actionId}. */
    public void createActionLabel(@Nonnull String text, @Nonnull String actionId) {
        myBuilder.withAction(LocalizeValue.of(text), actionId);
    }

    /**
     * Adds a clickable label triggering the action registered under {@code actionId}, running it in
     * {@link #NOTIFICATION_PANEL_PLACE} with {@code project} and {@code file} as its data context. Use this
     * instead of {@link #createActionLabel(String, String)} when the action behaves differently depending
     * on where it was invoked from, or when it needs the file the notification belongs to.
     */
    public void createActionLabel(
        @Nonnull String text,
        @Nonnull String actionId,
        @Nonnull Project project,
        @Nonnull VirtualFile file
    ) {
        ComponentEventListener<Component, ComponentEvent<Component>> listener = event -> {
            AnAction action = ActionManager.getInstance().getAction(actionId);
            if (action == null) return;
            DataContext dataContext = DataContext.builder()
                .add(Project.KEY, project)
                .add(VirtualFile.KEY, file)
                .build();
            action.actionPerformed(
                AnActionEvent.createFromAnAction(action, null, NOTIFICATION_PANEL_PLACE, dataContext)
            );
        };
        myBuilder.withAction(LocalizeValue.of(text), listener);
    }

    @Nonnull
    public String getActionPlace() {
        return NOTIFICATION_PANEL_PLACE;
    }
}
