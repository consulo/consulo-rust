/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsLiteralKind;
import org.rust.lang.core.psi.RsTokenType;

/**
 * Automatically deletes matching '#' characters for raw string literals.
 */
public class RsRawLiteralHashesDeleter extends RsEnableableBackspaceHandlerDelegate {
    @Nullable
    private Pair<TextRange, TextRange> myOffsets;

    @Override
    public boolean deleting(char c, PsiFile file, Editor editor) {
        int caretOffset = editor.getCaretModel().getOffset();
        if (!TypingUtil.isValidOffset(caretOffset, editor.getDocument().getCharsSequence())) return false;

        EditorEx editorEx = (EditorEx) editor;
        HighlighterIterator iterator = editorEx.getHighlighter().createIterator(caretOffset - 1);

        if (c != '#' || !RsTokenType.RS_RAW_LITERALS.contains(iterator.getTokenType())) return false;

        myOffsets = getHashesOffsets(iterator);
        return myOffsets != null;
    }

    @Override
    public boolean deleted(char c, PsiFile file, Editor editor) {
        int caretOffset = editor.getCaretModel().getOffset() + 1;
        Pair<TextRange, TextRange> offsets = myOffsets;
        if (offsets == null) return false;

        TextRange openHashes = offsets.getFirst();
        TextRange closeHashes = offsets.getSecond();

        if (caretOffset >= openHashes.getStartOffset() && caretOffset <= openHashes.getEndOffset() + 1) {
            TypingUtil.deleteChar(editor.getDocument(), closeHashes.getEndOffset() - 2);
        } else if (caretOffset >= closeHashes.getStartOffset() && caretOffset <= closeHashes.getEndOffset() + 1) {
            TypingUtil.deleteChar(editor.getDocument(), openHashes.getStartOffset());
        }

        return false;
    }

    @Nullable
    private static Pair<TextRange, TextRange> getHashesOffsets(HighlighterIterator iterator) {
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
