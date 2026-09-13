/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions.addFmtStringArgument;

import consulo.language.editor.impl.intention.QuickEditAction;
import consulo.disposer.Disposable;
import consulo.document.Document;
import consulo.codeEditor.Editor;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.util.EditorUtil;
import consulo.project.Project;
import consulo.ui.ex.popup.Balloon;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.disposer.Disposer;
import consulo.language.editor.ui.awt.EditorTextField;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import jakarta.annotation.Nonnull;
import org.rust.ide.intentions.util.macros.IntentionInMacroUtil;
import org.rust.lang.RsFileType;
import org.rust.lang.core.psi.impl.RsCodeFragment;
import org.rust.openapiext.DocumentExtUtil;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public final class RsAddFmtStringArgumentPopup {
    private RsAddFmtStringArgumentPopup() {
    }

    public static void show(@Nonnull Editor editor, @Nonnull Project project, @Nonnull RsCodeFragment codeFragment, @Nonnull Runnable onComplete) {
        EditorTextField editorTextField = createEditorTextField(project, codeFragment);
        if (editorTextField == null) return;
        showBalloon(editor, project, editorTextField, onComplete);
    }

    @org.jetbrains.annotations.Nullable
    private static EditorTextField createEditorTextField(@Nonnull Project project, @Nonnull RsCodeFragment codeFragment) {
        Document document = DocumentExtUtil.getDocument(codeFragment.getContainingFile());
        if (document == null) return null;
        EditorTextField editorTextField = new RsAddFmtStringArgumentEditorTextField(project, document);
        editorTextField.setFontInheritedFromLAF(false);
        // editorTextField.setFont(EditorUtil.getEditorFont()); — not available in Consulo
        return editorTextField;
    }

    private static void showBalloon(@Nonnull Editor editor, @Nonnull Disposable parent, @Nonnull EditorTextField editorTextField, @Nonnull Runnable onComplete) {
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
            public void documentChanged(@Nonnull DocumentEvent event) {
                int textWidth = fontMetrics.stringWidth(editorTextField.getText());
                editorTextField.setPreferredWidth(minimalWidth + textWidth);
                balloon.revalidate();
            }
        };
        editorTextField.addDocumentListener(documentListener);

        balloon.addListener(new JBPopupListener() {
            @Override
            public void onClosed(@Nonnull LightweightWindowEvent event) {
                editorTextField.removeKeyListener(keyListener);
                editorTextField.removeDocumentListener(documentListener);
            }
        });

        Editor realEditor = IntentionInMacroUtil.unwrapEditor(editor);

        Balloon.Position position = QuickEditAction.getBalloonPosition(realEditor);
        RelativePoint point = JBPopupFactory.getInstance().guessBestPopupLocation(realEditor.getContentComponent());
        if (position == Balloon.Position.above) {
            Point p = point.getPoint();
            point = new RelativePoint(point.getComponent(), new Point(p.x, p.y - realEditor.getLineHeight()));
        }
        balloon.show(point, position);
        editorTextField.requestFocus();
    }
}
