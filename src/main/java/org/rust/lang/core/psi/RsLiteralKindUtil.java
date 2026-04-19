/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsTokenType;

/**
 * Utility methods for computing literal offsets (for number and text literals).
 */
public final class RsLiteralKindUtil {

    private RsLiteralKindUtil() {
    }

    /**
     * Returns the kind of a literal expression, or null if the literal node is not found.
     */
    @org.jetbrains.annotations.Nullable
    public static RsLiteralKind getKind(@NotNull RsLitExpr litExpr) {
        ASTNode literalAstNode = litExpr.getNode().findChildByType(RsTokenType.RS_LITERALS);
        if (literalAstNode == null) return null;
        RsLiteralKind kind = RsLiteralKind.fromAstNode(literalAstNode);
        if (kind == null) {
            throw new IllegalStateException("Unknown literal: " + literalAstNode + " (`" + litExpr.getText() + "`)");
        }
        return kind;
    }

    @NotNull
    public static LiteralOffsets offsetsForNumber(@NotNull ASTNode node) {
        String text = node.getText();
        int start;
        String digits;
        if (text.length() >= 2) {
            String prefix = text.substring(0, 2);
            switch (prefix) {
                case "0b":
                    start = 2;
                    digits = "01";
                    break;
                case "0o":
                    start = 2;
                    digits = "012345678";
                    break;
                case "0x":
                    start = 2;
                    digits = "0123456789abcdefABCDEF";
                    break;
                default:
                    start = 0;
                    digits = "0123456789";
                    break;
            }
        } else {
            start = 0;
            digits = "0123456789";
        }

        boolean hasExponent = false;
        String sub = text.substring(start);
        for (int i = 0; i < sub.length(); i++) {
            char ch = sub.charAt(i);
            if (!hasExponent && (ch == 'e' || ch == 'E')) {
                hasExponent = true;
            } else if (digits.indexOf(ch) < 0 && ch != '+' && ch != '-' && ch != '_' && ch != '.') {
                return new LiteralOffsets(
                    TextRange.create(0, i + start),
                    null, null, null,
                    TextRange.create(i + start, node.getTextLength())
                );
            }
        }
        return new LiteralOffsets(TextRange.create(0, text.length()), null, null, null, null);
    }

    @NotNull
    public static LiteralOffsets offsetsForText(@NotNull ASTNode node) {
        IElementType elementType = node.getElementType();
        if (elementType == RsElementTypes.RAW_STRING_LITERAL
            || elementType == RsElementTypes.RAW_BYTE_STRING_LITERAL
            || elementType == RsElementTypes.RAW_CSTRING_LITERAL) {
            return offsetsForRawText(node);
        }

        String text = node.getText();
        char quote;
        if (elementType == RsElementTypes.BYTE_LITERAL || elementType == RsElementTypes.CHAR_LITERAL) {
            quote = '\'';
        } else {
            quote = '"';
        }

        int prefixEnd = locatePrefix(node);
        int textLength = node.getTextLength();

        int openDelimEnd = prefixEnd < textLength ? prefixEnd + 1 : prefixEnd;
        // assert text[prefixEnd] == quote

        int valueEnd = openDelimEnd;
        boolean escape = false;
        for (int i = openDelimEnd; i < textLength; i++) {
            char ch = text.charAt(i);
            if (escape) {
                escape = false;
            } else if (ch == '\\') {
                escape = true;
            } else if (ch == quote) {
                valueEnd = i;
                break;
            }
            if (i == textLength - 1) {
                valueEnd = textLength;
            }
        }
        if (openDelimEnd == textLength) {
            valueEnd = textLength;
        }

        int closeDelimEnd = valueEnd < textLength ? valueEnd + 1 : valueEnd;

        return LiteralOffsets.fromEndOffsets(prefixEnd, openDelimEnd, valueEnd, closeDelimEnd, textLength);
    }

    @NotNull
    private static LiteralOffsets offsetsForRawText(@NotNull ASTNode node) {
        String text = node.getText();
        int textLength = node.getTextLength();

        int prefixEnd = locatePrefix(node);

        int hashes = 0;
        int pos = prefixEnd;
        while (pos < textLength && text.charAt(pos) == '#') {
            pos++;
            hashes++;
        }

        int openDelimEnd;
        if (prefixEnd < textLength) {
            openDelimEnd = prefixEnd + 1 + hashes;
        } else {
            openDelimEnd = prefixEnd;
        }

        int valueEnd = openDelimEnd;
        for (int i = openDelimEnd; i < textLength; i++) {
            char ch = text.charAt(i);
            if (ch == '"' && i + hashes < textLength) {
                boolean allHashes = true;
                for (int j = 1; j <= hashes; j++) {
                    if (text.charAt(i + j) != '#') {
                        allHashes = false;
                        break;
                    }
                }
                if (allHashes) {
                    valueEnd = i;
                    break;
                }
            }
            if (i == textLength - 1) {
                valueEnd = textLength;
            }
        }
        if (openDelimEnd >= textLength) {
            valueEnd = textLength;
        }

        int closeDelimEnd;
        if (valueEnd < textLength) {
            closeDelimEnd = valueEnd + 1 + hashes;
        } else {
            closeDelimEnd = valueEnd;
        }

        return LiteralOffsets.fromEndOffsets(prefixEnd, openDelimEnd, valueEnd, closeDelimEnd, textLength);
    }

    private static int locatePrefix(@NotNull ASTNode node) {
        String text = node.getText();
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isLetter(text.charAt(i))) {
                return i;
            }
        }
        return node.getTextLength();
    }
}
