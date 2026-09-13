/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.HighlighterIterator;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiFile;
import consulo.util.lang.Pair;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.impl.RsLiteralKind;
import org.rust.lang.core.psi.impl.RsTokenSets;
import consulo.language.ast.IElementType;

/**
 * Automatically deletes matching '#' characters for raw string literals.
 */
@ExtensionImpl(id = "RsRawLiteralHashesDeleter")
public class RsRawLiteralHashesDeleter extends RsEnableableBackspaceHandlerDelegate {
    @Nullable
    private Pair<TextRange, TextRange> myOffsets;

    @Override
    public boolean deleting(char c, PsiFile file, Editor editor) {
        int caretOffset = editor.getCaretModel().getOffset();
        if (!TypingUtil.isValidOffset(caretOffset, editor.getDocument().getCharsSequence())) return false;

        EditorEx editorEx = (EditorEx) editor;
        HighlighterIterator iterator = editorEx.getHighlighter().createIterator(caretOffset - 1);

        if (c != '#' || !RsTokenSets.RS_RAW_LITERALS.contains(((IElementType) iterator.getTokenType()))) return false;

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
        if (!RsTokenSets.RS_RAW_LITERALS.contains(literal.getNode().getElementType())) return null;
        TextRange openDelim = literal.getOffsets().getOpenDelim();
        TextRange closeDelim = literal.getOffsets().getCloseDelim();
        if (openDelim == null || closeDelim == null) return null;

        TextRange openRange = openDelim.shiftRight(iterator.getStart()).grown(-1);
        TextRange closeRange = closeDelim.shiftRight(iterator.getStart() + 1).grown(-1);
        return new Pair<>(openRange, closeRange);
    }
}
