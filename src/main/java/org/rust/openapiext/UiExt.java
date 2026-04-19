/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.NlsContexts.DialogTitle;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.dsl.builder.Cell;
import com.intellij.ui.dsl.builder.Row;
import com.intellij.ui.dsl.gridLayout.HorizontalAlign;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.intentions.util.macros.RsIntentionInsideMacroExpansionEditor;
import org.rust.lang.RsFileType;
import org.rust.lang.core.psi.ext.RsElement;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.util.function.Consumer;

public final class UiExt {
    private UiExt() {
    }

    @NotNull
    public static TextFieldWithBrowseButton pathToDirectoryTextField(
        @NotNull Disposable disposable,
        @DialogTitle @NotNull String title,
        @Nullable Runnable onTextChanged
    ) {
        return pathTextField(
            FileChooserDescriptorFactory.createSingleFolderDescriptor(),
            disposable,
            title,
            onTextChanged
        );
    }

    @NotNull
    public static TextFieldWithBrowseButton pathToRsFileTextField(
        @NotNull Disposable disposable,
        @DialogTitle @NotNull String title,
        @NotNull Project project,
        @Nullable Runnable onTextChanged
    ) {
        return pathTextField(
            FileChooserDescriptorFactory
                .createSingleFileDescriptor(RsFileType.INSTANCE)
                .withRoots(ProjectUtil.guessProjectDir(project)),
            disposable,
            title,
            onTextChanged
        );
    }

    @NotNull
    public static TextFieldWithBrowseButton pathTextField(
        @NotNull FileChooserDescriptor fileChooserDescriptor,
        @NotNull Disposable disposable,
        @DialogTitle @NotNull String title,
        @Nullable Runnable onTextChanged
    ) {
        TextFieldWithBrowseButton component = new TextFieldWithBrowseButton(null, disposable);
        component.addBrowseFolderListener(
            title, null, null,
            fileChooserDescriptor,
            TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
        );
        if (onTextChanged != null) {
            addTextChangeListener(component.getChildComponent(), e -> onTextChanged.run());
        }
        return component;
    }

    public static void addTextChangeListener(@NotNull JTextField textField, @NotNull Consumer<DocumentEvent> listener) {
        textField.getDocument().addDocumentListener(
            new DocumentAdapter() {
                @Override
                protected void textChanged(@NotNull DocumentEvent e) {
                    listener.accept(e);
                }
            }
        );
    }

    public static void selectElement(@NotNull RsElement element, @NotNull Editor editor) {
        int start = element.getTextRange().getStartOffset();
        Editor unwrappedEditor;
        if (editor instanceof RsIntentionInsideMacroExpansionEditor) {
            RsIntentionInsideMacroExpansionEditor macroEditor = (RsIntentionInsideMacroExpansionEditor) editor;
            if (element.getContainingFile() != macroEditor.getPsiFileCopy()) {
                if (element.getContainingFile() != macroEditor.getOriginalFile()) return;
                unwrappedEditor = macroEditor.getOriginalEditor();
            } else {
                unwrappedEditor = editor;
            }
        } else {
            unwrappedEditor = editor;
        }
        unwrappedEditor.getCaretModel().moveToOffset(start);
        unwrappedEditor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
        unwrappedEditor.getSelectionModel().setSelection(start, element.getTextRange().getEndOffset());
    }

    @NotNull
    public static <T extends JComponent> Cell<T> fullWidthCell(@NotNull Row row, @NotNull T component) {
        return row.cell(component)
            .horizontalAlign(HorizontalAlign.FILL);
    }

    @NotNull
    public static String getTrimmedText(@NotNull JBTextField textField) {
        return textField.getText().trim();
    }
}
