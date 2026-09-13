/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing;

import consulo.document.Document;
import consulo.codeEditor.HighlighterIterator;
import consulo.language.impl.psi.LeafPsiElement;
import consulo.language.ast.IElementType;
import consulo.util.lang.CharSequenceSubSequence;
import consulo.util.lang.Pair;
import org.rust.lang.core.psi.impl.RsLiteralKind;

public final class RsTypingUtils {

    private RsTypingUtils() {
    }

    public static boolean isValidOffset(int offset, CharSequence text) {
        return 0 <= offset && offset <= text.length();
    }

    /**
     * Beware that this returns {@code false} for EOF!
     */
    public static boolean isValidInnerOffset(int offset, CharSequence text) {
        return 0 <= offset && offset < text.length();
    }

    /**
     * Get previous and next token types relative to iterator position.
     */
    public static Pair<IElementType, IElementType> getSiblingTokens(HighlighterIterator iterator) {
        iterator.retreat();
        IElementType prev = iterator.atEnd() ? null : ((consulo.language.ast.IElementType) iterator.getTokenType());
        iterator.advance();

        iterator.advance();
        IElementType next = iterator.atEnd() ? null : ((consulo.language.ast.IElementType) iterator.getTokenType());
        iterator.retreat();

        return new Pair<>(prev, next);
    }

    /**
     * Creates virtual {@link RsLiteralKind} PSI element assuming that it is represented as
     * single, contiguous token in highlighter, in other words - it doesn't contain
     * any escape sequences etc. (hence 'dumb').
     */
    public static RsLiteralKind.RsComplexLiteral getLiteralDumb(HighlighterIterator iterator) {
        int start = iterator.getStart();
        int end = iterator.getEnd();

        Document document = iterator.getDocument();
        CharSequence text = document.getCharsSequence();
        CharSequence literalText = new CharSequenceSubSequence(text, start, end);

        IElementType elementType = ((consulo.language.ast.IElementType) iterator.getTokenType());
        if (elementType == null) return null;
        Object result = RsLiteralKind.fromAstNode(new LeafPsiElement(elementType, literalText));
        if (result instanceof RsLiteralKind.RsComplexLiteral) {
            return (RsLiteralKind.RsComplexLiteral) result;
        }
        return null;
    }

    public static void deleteChar(Document document, int offset) {
        document.deleteString(offset, offset + 1);
    }

    public static boolean endsWithUnescapedBackslash(CharSequence text) {
        int count = 0;
        for (int i = text.length() - 1; i >= 0; i--) {
            if (text.charAt(i) == '\\') {
                count++;
            } else {
                break;
            }
        }
        return count % 2 == 1;
    }
}
