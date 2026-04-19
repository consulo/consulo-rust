/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsFile;

/**
 * Brace handler infrastructure for Rust.
 */
public final class RsBraceHandlers {

    private RsBraceHandlers() {
    }

    public static class BraceKind {
        private final char myChar;
        @NotNull
        private final IElementType myTokenType;

        public BraceKind(char c, @NotNull IElementType tokenType) {
            myChar = c;
            myTokenType = tokenType;
        }

        public char getChar() {
            return myChar;
        }

        @NotNull
        public IElementType getTokenType() {
            return myTokenType;
        }
    }

    public interface BraceHandler {
        @NotNull
        BraceKind getOpening();

        @NotNull
        BraceKind getClosing();

        boolean shouldComplete(@NotNull Editor editor);

        int calculateBalance(@NotNull Editor editor);
    }

    public static class RsBraceTypedHandler extends TypedHandlerDelegate {
        @NotNull
        private final BraceHandler myHandler;
        private boolean myOpeningTyped = false;

        public RsBraceTypedHandler(@NotNull BraceHandler handler) {
            myHandler = handler;
        }

        @NotNull
        @Override
        public Result beforeCharTyped(char c, @NotNull Project project, @NotNull Editor editor,
                                      @NotNull PsiFile file, @NotNull FileType fileType) {
            if (!(file instanceof RsFile)) return Result.CONTINUE;

            if (CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET) {
                if (c == myHandler.getOpening().getChar()) {
                    myOpeningTyped = myHandler.shouldComplete(editor);
                } else if (c == myHandler.getClosing().getChar()) {
                    HighlighterIterator lexer = TypingUtil.createLexer(editor, editor.getCaretModel().getOffset());
                    if (lexer == null) return Result.CONTINUE;
                    IElementType tokenType = lexer.getTokenType();
                    if (tokenType == myHandler.getClosing().getTokenType() && myHandler.calculateBalance(editor) == 0) {
                        EditorModificationUtil.moveCaretRelatively(editor, 1);
                        return Result.STOP;
                    }
                }
            }

            return Result.CONTINUE;
        }

        @NotNull
        @Override
        public Result charTyped(char c, @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
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

    public static abstract class RsBraceBackspaceHandler extends RsEnableableBackspaceHandlerDelegate {
        @NotNull
        private final BraceHandler myHandler;

        public RsBraceBackspaceHandler(@NotNull BraceHandler handler) {
            myHandler = handler;
        }

        @Override
        public boolean deleting(char c, PsiFile file, Editor editor) {
            if (c == myHandler.getOpening().getChar() && file instanceof RsFile) {
                int offset = editor.getCaretModel().getOffset();
                HighlighterIterator iterator = ((EditorEx) editor).getHighlighter().createIterator(offset);
                return iterator.getTokenType() == myHandler.getClosing().getTokenType();
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
}
