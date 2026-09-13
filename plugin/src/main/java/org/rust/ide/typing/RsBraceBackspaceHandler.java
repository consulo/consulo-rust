/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.HighlighterIterator;
import consulo.language.psi.PsiFile;
import org.rust.lang.core.psi.impl.RsFile;
import consulo.language.ast.IElementType;

public abstract class RsBraceBackspaceHandler extends RsEnableableBackspaceHandlerDelegate {

    private final BraceHandler myHandler;

    protected RsBraceBackspaceHandler(BraceHandler handler) {
        myHandler = handler;
    }

    @Override
    public boolean deleting(char c, PsiFile file, Editor editor) {
        if (c == myHandler.getOpening().getChar() && file instanceof RsFile) {
            int offset = editor.getCaretModel().getOffset();
            HighlighterIterator iterator = ((EditorEx) editor).getHighlighter().createIterator(offset);
            return ((consulo.language.ast.IElementType) iterator.getTokenType()) == myHandler.getClosing().getTokenType();
        }
        return false;
    }

    @Override
    public boolean deleted(char c, PsiFile file, Editor editor) {
        int balance = myHandler.calculateBalance(editor);
        if (balance < 0) {
            int offset = editor.getCaretModel().getOffset();
            editor.getDocument().deleteString(offset, offset + 1);
            return true;
        }
        return true;
    }
}
