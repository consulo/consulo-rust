/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.EditorEx;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.text.CharSequenceSubSequence;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsLiteralKind;

public final class TypingUtil {

    private TypingUtil() {
    }

    public static boolean isValidOffset(int offset, @NotNull CharSequence text) {
        return 0 <= offset && offset <= text.length();
    }

    /**
     * Beware that this returns {@code false} for EOF!
     */
    public static boolean isValidInnerOffset(int offset, @NotNull CharSequence text) {
        return 0 <= offset && offset < text.length();
    }

    /**
     * Get previous and next token types relative to {@code iterator} position.
     */
    @NotNull
    public static Pair<IElementType, IElementType> getSiblingTokens(@NotNull HighlighterIterator iterator) {
        iterator.retreat();
        IElementType prev = iterator.atEnd() ? null : iterator.getTokenType();
        iterator.advance();

        iterator.advance();
        IElementType next = iterator.atEnd() ? null : iterator.getTokenType();
        iterator.retreat();

        return new Pair<>(prev, next);
    }

    /**
     * Creates virtual {@link RsLiteralKind} PSI element assuming that it is represented as
     * single, contiguous token in highlighter, in other words - it doesn't contain
     * any escape sequences etc. (hence 'dumb').
     */
    @Nullable
    public static RsLiteralKind.RsComplexLiteral getLiteralDumb(@NotNull HighlighterIterator iterator) {
        int start = iterator.getStart();
        int end = iterator.getEnd();

        Document document = iterator.getDocument();
        CharSequence text = document.getCharsSequence();
        CharSequence literalText = new CharSequenceSubSequence(text, start, end);

        IElementType elementType = iterator.getTokenType();
        if (elementType == null) return null;
        return (RsLiteralKind.RsComplexLiteral) RsLiteralKind.fromAstNode(new LeafPsiElement(elementType, literalText));
    }

    public static void deleteChar(@NotNull Document document, int offset) {
        document.deleteString(offset, offset + 1);
    }

    public static boolean endsWithUnescapedBackslash(@NotNull CharSequence text) {
        int count = 0;
        for (int i = text.length() - 1; i >= 0 && text.charAt(i) == '\\'; i--) {
            count++;
        }
        return count % 2 == 1;
    }

    @Nullable
    public static HighlighterIterator createLexer(@NotNull Editor editor, int offset) {
        if (!isValidOffset(offset, editor.getDocument().getCharsSequence())) return null;
        HighlighterIterator lexer = ((EditorEx) editor).getHighlighter().createIterator(offset);
        if (lexer.atEnd()) return null;
        return lexer;
    }
}
