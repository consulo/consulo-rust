/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing;

import consulo.language.editor.CodeInsightSettings;
import consulo.language.editor.action.TypedHandlerDelegate;
import consulo.codeEditor.Editor;
import consulo.codeEditor.util.EditorModificationUtil;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.HighlighterIterator;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.project.Project;
import consulo.language.psi.PsiFile;
import consulo.language.ast.IElementType;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.impl.RsFile;

/**
 * Brace handler infrastructure for Rust.
 */
public final class RsBraceHandlers {

    private RsBraceHandlers() {
    }

    public static class BraceKind {
        private final char myChar;
        @Nonnull
        private final IElementType myTokenType;

        public BraceKind(char c, @Nonnull IElementType tokenType) {
            myChar = c;
            myTokenType = tokenType;
        }

        public char getChar() {
            return myChar;
        }

        @Nonnull
        public IElementType getTokenType() {
            return myTokenType;
        }
    }

    public interface BraceHandler {
        @Nonnull
        BraceKind getOpening();

        @Nonnull
        BraceKind getClosing();

        boolean shouldComplete(@Nonnull Editor editor);

        int calculateBalance(@Nonnull Editor editor);
    }

    public static class RsBraceTypedHandler extends TypedHandlerDelegate {
        @Nonnull
        private final BraceHandler myHandler;
        private boolean myOpeningTyped = false;

        public RsBraceTypedHandler(@Nonnull BraceHandler handler) {
            myHandler = handler;
        }

        @Nonnull
        @Override
        public Result beforeCharTyped(char c, @Nonnull Project project, @Nonnull Editor editor,
                                      @Nonnull PsiFile file, @Nonnull FileType fileType) {
            if (!(file instanceof RsFile)) return Result.CONTINUE;

            if (CodeInsightSettings.getInstance().AUTOINSERT_PAIR_BRACKET) {
                if (c == myHandler.getOpening().getChar()) {
                    myOpeningTyped = myHandler.shouldComplete(editor);
                } else if (c == myHandler.getClosing().getChar()) {
                    HighlighterIterator lexer = TypingUtil.createLexer(editor, editor.getCaretModel().getOffset());
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

        @Nonnull
        @Override
        public Result charTyped(char c, @Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
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
        @Nonnull
        private final BraceHandler myHandler;

        public RsBraceBackspaceHandler(@Nonnull BraceHandler handler) {
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
}
