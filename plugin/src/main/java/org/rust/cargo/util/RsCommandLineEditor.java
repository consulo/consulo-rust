/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.util;

import consulo.ui.ex.awt.TextAccessor;
import consulo.language.editor.ui.awt.TextFieldCompletionProvider;
import consulo.project.Project;
import consulo.ui.ex.awt.JBTextField;

import javax.swing.JPanel;
import java.awt.BorderLayout;

/**
 * Simplified Consulo port of the original IntelliJ RsCommandLineEditor.
 * The original built an {@code ExpandableEditorSupport}-backed popup editor over an
 * {@code EditorTextField} with completion. Consulo's {@code TextFieldWithCompletion}
 * takes {@code TextCompletionProvider} (not {@code TextFieldCompletionProvider}),
 * and the IntelliJ-specific ExpandableEditorSupport Content / prepare API is absent.
 * For now this falls back to a plain text field — completion is not wired.
 */
public class RsCommandLineEditor extends JPanel implements TextAccessor {

    private final JBTextField textField;

    public RsCommandLineEditor(Project project, TextFieldCompletionProvider completionProvider) {
        super(new BorderLayout());
        this.textField = new JBTextField();
        add(textField, BorderLayout.CENTER);
    }

    @Override
    public void setText(String text) {
        textField.setText(text);
    }

    public String getText() {
        return textField.getText();
    }
}
