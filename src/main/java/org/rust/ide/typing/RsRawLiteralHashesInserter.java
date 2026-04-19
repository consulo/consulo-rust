/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing;

import com.intellij.codeInsight.daemon.impl.CollectHighlightsUtil;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.RsLiteralKind;
import org.rust.lang.core.psi.RsTokenType;

import java.util.List;

/**
 * Automatically inserts matching '#' characters for raw string literals.
 */
public class RsRawLiteralHashesInserter extends TypedHandlerDelegate {
    @NotNull
    @Override
    public Result beforeCharTyped(char c, @NotNull Project project, @NotNull Editor editor,
                                  @NotNull PsiFile file, @NotNull FileType fileType) {
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

    private boolean hasErrorAfterLiteral(@NotNull PsiFile file, int start, @NotNull Editor editor) {
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

    private static Pair<TextRange, TextRange> getHashesOffsets(@NotNull HighlighterIterator iterator) {
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
