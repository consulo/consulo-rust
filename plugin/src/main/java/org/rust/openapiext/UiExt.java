/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import consulo.disposer.Disposable;
import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollType;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.project.Project;
import consulo.project.util.ProjectUtil;
import consulo.ui.ex.awt.TextComponentAccessor;
import consulo.ui.ex.awt.TextFieldWithBrowseButton;

import consulo.ui.ex.awt.event.DocumentAdapter;
import consulo.ui.ex.awt.JBTextField;
import com.intellij.ui.dsl.builder.Cell;
import com.intellij.ui.dsl.builder.Row;
import com.intellij.ui.dsl.gridLayout.HorizontalAlign;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.ide.intentions.util.macros.RsIntentionInsideMacroExpansionEditor;
import org.rust.lang.RsFileType;
import org.rust.lang.core.psi.ext.RsElement;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.util.function.Consumer;

public final class UiExt {
    private UiExt() {
    }

    @Nonnull
    public static TextFieldWithBrowseButton pathToDirectoryTextField(
        @Nonnull Disposable disposable,
         @Nonnull String title,
        @Nullable Runnable onTextChanged
    ) {
        return pathTextField(
            FileChooserDescriptorFactory.createSingleFolderDescriptor(),
            disposable,
            title,
            onTextChanged
        );
    }

    @Nonnull
    public static TextFieldWithBrowseButton pathToRsFileTextField(
        @Nonnull Disposable disposable,
         @Nonnull String title,
        @Nonnull Project project,
        @Nullable Runnable onTextChanged
    ) {
        return pathTextField(
            FileChooserDescriptorFactory
                .createSingleFileDescriptor(RsFileType.INSTANCE)
                .withRoots(project.getBaseDir()),
            disposable,
            title,
            onTextChanged
        );
    }

    @Nonnull
    public static TextFieldWithBrowseButton pathTextField(
        @Nonnull FileChooserDescriptor fileChooserDescriptor,
        @Nonnull Disposable disposable,
         @Nonnull String title,
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

    public static void addTextChangeListener(@Nonnull JTextField textField, @Nonnull Consumer<DocumentEvent> listener) {
        textField.getDocument().addDocumentListener(
            new DocumentAdapter() {
                @Override
                protected void textChanged(@Nonnull DocumentEvent e) {
                    listener.accept(e);
                }
            }
        );
    }

    public static void selectElement(@Nonnull RsElement element, @Nonnull Editor editor) {
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

    @Nonnull
    public static <T extends JComponent> Cell<T> fullWidthCell(@Nonnull Row row, @Nonnull T component) {
        return row.cell(component)
            .horizontalAlign(HorizontalAlign.FILL);
    }

    @Nonnull
    public static String getTrimmedText(@Nonnull JBTextField textField) {
        return textField.getText().trim();
    }
}
