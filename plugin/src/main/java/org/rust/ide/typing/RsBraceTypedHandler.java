/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing;

import consulo.language.editor.CodeInsightSettings;
import consulo.language.editor.action.TypedHandlerDelegate;
import consulo.codeEditor.Editor;
import consulo.codeEditor.util.EditorModificationUtil;
import consulo.codeEditor.HighlighterIterator;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.project.Project;
import consulo.language.psi.PsiFile;
import consulo.language.ast.IElementType;
import org.rust.lang.core.psi.impl.RsFile;

public abstract class RsBraceTypedHandler extends TypedHandlerDelegate {

    private final BraceHandler myHandler;
    private boolean myOpeningTyped = false;

    protected RsBraceTypedHandler(BraceHandler handler) {
        myHandler = handler;
    }

    @Override
    public Result beforeCharTyped(char c, Project project, Editor editor, PsiFile file, FileType fileType) {
        if (!(file instanceof RsFile)) return Result.CONTINUE;

        if (CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET) {
            if (c == myHandler.getOpening().getChar()) {
                myOpeningTyped = myHandler.shouldComplete(editor);
            } else if (c == myHandler.getClosing().getChar()) {
                HighlighterIterator lexer = RsBraceHandlersUtil.createLexer(editor, editor.getCaretModel().getOffset());
                if (lexer == null) return Result.CONTINUE;
                IElementType tokenType = (IElementType) lexer.getTokenType();
                if (tokenType == myHandler.getClosing().getTokenType() && myHandler.calculateBalance(editor) == 0) {
                    EditorModificationUtil.moveCaretRelatively(editor, 1);
                    return Result.STOP;
                }
            }
        }

        return Result.CONTINUE;
    }

    @Override
    public Result charTyped(char c, Project project, Editor editor, PsiFile file) {
        if (!(file instanceof RsFile)) return Result.CONTINUE;

        if (myOpeningTyped) {
            myOpeningTyped = false;
            int balance = myHandler.calculateBalance(editor);
            if (balance == 1) {
                int offset = editor.getCaretModel().getOffset();
                editor.getDocument().insertString(offset, String.valueOf(myHandler.getClosing().getChar()));
            }
        }

        return super.charTyped(c, project, editor, file);
    }
}
