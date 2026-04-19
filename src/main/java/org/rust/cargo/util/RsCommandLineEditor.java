/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.util;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.project.Project;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.ExpandableEditorSupport;
import com.intellij.ui.TextAccessor;
import com.intellij.util.Function;
import com.intellij.util.TextFieldCompletionProvider;
import com.intellij.util.textCompletion.TextFieldWithCompletion;

import javax.swing.*;
import java.awt.*;

public class RsCommandLineEditor extends JPanel implements TextAccessor {

    private final Project project;
    private final TextFieldCompletionProvider completionProvider;
    private final TextFieldWithCompletion textField;

    public RsCommandLineEditor(Project project, TextFieldCompletionProvider completionProvider) {
        super(new BorderLayout());
        this.project = project;
        this.completionProvider = completionProvider;
        this.textField = createTextField("");
        new ExpandableEditorSupportWithCustomPopup(textField);
        add(textField, BorderLayout.CENTER);
    }

    @Override
    public void setText(String text) {
        textField.setText(text);
    }

    @Override
    public String getText() {
        return textField.getText();
    }

    private TextFieldWithCompletion createTextField(String value) {
        return new TextFieldWithCompletion(
            project,
            completionProvider,
            value,
            true,
            false,
            false
        );
    }

    private class ExpandableEditorSupportWithCustomPopup extends ExpandableEditorSupport {

        ExpandableEditorSupportWithCustomPopup(EditorTextField field) {
            super(field);
        }

        @SuppressWarnings("UnstableApiUsage")
        @Override
        protected Content prepare(EditorTextField field, Function<? super String, String> onShow) {
            EditorTextField popup = createTextField(onShow.fun(field.getText()));
            Color background = field.getBackground();

            popup.setBackground(background);
            popup.setOneLineMode(false);
            popup.setPreferredSize(new Dimension(field.getWidth(), 5 * field.getHeight()));
            popup.addSettingsProvider(editor -> {
                initPopupEditor(editor, background);
                copyCaretPosition(editor, field.getEditor());
            });

            return new Content() {
                @Override
                public JComponent getContentComponent() {
                    return popup;
                }

                @Override
                public JComponent getFocusableComponent() {
                    return popup;
                }

                @Override
                public void cancel(Function<? super String, String> onHide) {
                    field.setText(onHide.fun(popup.getText()));
                    Editor editor = field.getEditor();
                    if (editor != null) copyCaretPosition(editor, popup.getEditor());
                    if (editor instanceof EditorEx) updateFieldFolding((EditorEx) editor);
                }
            };
        }
    }

    private static void copyCaretPosition(Editor destination, Editor source) {
        if (source == null) return; // unexpected
        try {
            destination.getCaretModel().setCaretsAndSelections(source.getCaretModel().getCaretsAndSelections());
        } catch (IllegalArgumentException ignored) {
        }
    }
}
