/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions.addFmtStringArgument;

import com.intellij.codeInsight.intention.impl.QuickEditAction;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.rust.ide.intentions.util.macros.IntentionInMacroUtil;
import org.rust.lang.RsFileType;
import org.rust.lang.core.psi.RsCodeFragment;
import org.rust.openapiext.DocumentExtUtil;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public final class RsAddFmtStringArgumentPopup {
    private RsAddFmtStringArgumentPopup() {
    }

    public static void show(@NotNull Editor editor, @NotNull Project project, @NotNull RsCodeFragment codeFragment, @NotNull Runnable onComplete) {
        EditorTextField editorTextField = createEditorTextField(project, codeFragment);
        if (editorTextField == null) return;
        showBalloon(editor, project, editorTextField, onComplete);
    }

    @org.jetbrains.annotations.Nullable
    private static EditorTextField createEditorTextField(@NotNull Project project, @NotNull RsCodeFragment codeFragment) {
        Document document = DocumentExtUtil.getDocument(codeFragment.getContainingFile());
        if (document == null) return null;
        EditorTextField editorTextField = new RsAddFmtStringArgumentEditorTextField(project, document);
        editorTextField.setFontInheritedFromLAF(false);
        editorTextField.setFont(EditorUtil.getEditorFont());
        return editorTextField;
    }

    private static void showBalloon(@NotNull Editor editor, @NotNull Disposable parent, @NotNull EditorTextField editorTextField, @NotNull Runnable onComplete) {
        Balloon balloon = JBPopupFactory.getInstance().createBalloonBuilder(editorTextField)
            .setShadow(true)
            .setAnimationCycle(0)
            .setHideOnAction(false)
            .setHideOnKeyOutside(false)
            .setFillColor(UIUtil.getPanelBackground())
            .setBorderInsets(JBUI.insets(3))
            .createBalloon();
        Disposer.register(parent, balloon);

        KeyListener keyListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_ENTER:
                        balloon.hide();
                        onComplete.run();
                        break;
                    case KeyEvent.VK_ESCAPE:
                        balloon.hide();
                        break;
                }
            }
        };
        editorTextField.addKeyListener(keyListener);

        FontMetrics fontMetrics = editorTextField.getFontMetrics(editorTextField.getFont());
        int minimalWidth = fontMetrics.stringWidth("1234");
        editorTextField.setPreferredWidth(minimalWidth);
        DocumentListener documentListener = new DocumentListener() {
            @Override
            public void documentChanged(@NotNull DocumentEvent event) {
                int textWidth = fontMetrics.stringWidth(editorTextField.getText());
                editorTextField.setPreferredWidth(minimalWidth + textWidth);
                balloon.revalidate();
            }
        };
        editorTextField.addDocumentListener(documentListener);

        balloon.addListener(new JBPopupListener() {
            @Override
            public void onClosed(@NotNull LightweightWindowEvent event) {
                editorTextField.removeKeyListener(keyListener);
                editorTextField.removeDocumentListener(documentListener);
            }
        });

        Editor realEditor = IntentionInMacroUtil.unwrapEditor(editor);

        Balloon.Position position = QuickEditAction.getBalloonPosition(realEditor);
        RelativePoint point = JBPopupFactory.getInstance().guessBestPopupLocation(realEditor);
        if (position == Balloon.Position.above) {
            Point p = point.getPoint();
            point = new RelativePoint(point.getComponent(), new Point(p.x, p.y - realEditor.getLineHeight()));
        }
        balloon.show(point, position);
        editorTextField.requestFocus();
    }
}
