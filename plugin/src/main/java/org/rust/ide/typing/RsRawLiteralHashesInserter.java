/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing;

import consulo.language.editor.util.CollectHighlightsUtil;
import consulo.language.editor.action.TypedHandlerDelegate;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.HighlighterIterator;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.project.Project;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiFile;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.RsLiteralKind;
import org.rust.lang.core.psi.RsTokenType;

import java.util.List;

/**
 * Automatically inserts matching '#' characters for raw string literals.
 */
public class RsRawLiteralHashesInserter extends TypedHandlerDelegate {
    @Nonnull
    @Override
    public Result beforeCharTyped(char c, @Nonnull Project project, @Nonnull Editor editor,
                                  @Nonnull PsiFile file, @Nonnull FileType fileType) {
        if (!(file instanceof RsFile)) return Result.CONTINUE;
        if (c != '#') return Result.CONTINUE;

        int caretOffset = editor.getCaretModel().getOffset();
        if (!TypingUtil.isValidOffset(caretOffset - 1, editor.getDocument().getCharsSequence())) {
            return Result.CONTINUE;
        }

        EditorEx editorEx = (EditorEx) editor;
        HighlighterIterator iterator = editorEx.getHighlighter().createIterator(caretOffset - 1);
        Pair<TextRange, TextRange> offsets = getHashesOffsets(iterator);
        if (offsets == null) return Result.CONTINUE;

        TextRange openHashes = offsets.getFirst();
        TextRange closeHashes = offsets.getSecond();

        boolean hasErrorAfterLiteral = hasErrorAfterLiteral(file, closeHashes.getEndOffset(), editor);
        if (hasErrorAfterLiteral) return Result.CONTINUE;

        if (caretOffset >= openHashes.getStartOffset() && caretOffset <= openHashes.getEndOffset() + 1) {
            editor.getDocument().insertString(closeHashes.getStartOffset(), "#");
        } else if (caretOffset >= closeHashes.getStartOffset() && caretOffset <= closeHashes.getEndOffset() + 1) {
            editor.getDocument().insertString(openHashes.getEndOffset(), "#");
        }

        return Result.CONTINUE;
    }

    private boolean hasErrorAfterLiteral(@Nonnull PsiFile file, int start, @Nonnull Editor editor) {
        CharSequence chars = editor.getDocument().getCharsSequence();
        int end = chars.toString().indexOf('\n', start);
        if (end == -1) {
            end = chars.length();
        }
        List<PsiElement> elements = CollectHighlightsUtil.getElementsInRange(file, start, end);
        for (PsiElement element : elements) {
            if (element instanceof PsiErrorElement) return true;
        }
        return false;
    }

    private static Pair<TextRange, TextRange> getHashesOffsets(@Nonnull HighlighterIterator iterator) {
        RsLiteralKind.RsComplexLiteral literal = TypingUtil.getLiteralDumb(iterator);
        if (literal == null) return null;
        if (!RsTokenType.RS_RAW_LITERALS.contains(literal.getNode().getElementType())) return null;
        TextRange openDelim = literal.getOffsets().getOpenDelim();
        TextRange closeDelim = literal.getOffsets().getCloseDelim();
        if (openDelim == null || closeDelim == null) return null;

        TextRange openRange = openDelim.shiftRight(iterator.getStart()).grown(-1);
        TextRange closeRange = closeDelim.shiftRight(iterator.getStart() + 1).grown(-1);
        return new Pair<>(openRange, closeRange);
    }
}
