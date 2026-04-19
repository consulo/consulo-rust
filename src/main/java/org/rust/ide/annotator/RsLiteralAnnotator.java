/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator;

import com.intellij.lang.ASTNode;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.lang.core.psi.*;

import java.util.List;
import org.rust.lang.core.psi.RsLiteralKindUtil;

public class RsLiteralAnnotator extends AnnotatorBase {
    @Override
    protected void annotateInternal(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (!(element instanceof RsLitExpr)) return;
        RsLitExpr litExpr = (RsLitExpr) element;
        RsLiteralKind literal = RsLiteralKindUtil.getKind(litExpr);
        if (literal == null) return;

        // Check suffix
        if (literal instanceof RsLiteralKind.IntegerLiteral || literal instanceof RsLiteralKind.FloatLiteral
            || literal instanceof RsLiteralKind.StringLiteral || literal instanceof RsLiteralKind.CharLiteral) {
            RsLiteralKind.RsLiteralWithSuffix literalWithSuffix = (RsLiteralKind.RsLiteralWithSuffix) literal;
            String suffix = literalWithSuffix.getSuffix();
            List<String> validSuffixes = literalWithSuffix.getValidSuffixes();
            if (suffix != null && !suffix.isEmpty() && !validSuffixes.contains(suffix)) {
                String message;
                if (!validSuffixes.isEmpty()) {
                    StringBuilder validSuffixesStr = new StringBuilder();
                    for (int i = 0; i < validSuffixes.size(); i++) {
                        if (i > 0) validSuffixesStr.append(", ");
                        validSuffixesStr.append("'").append(validSuffixes.get(i)).append("'");
                    }
                    message = RsBundle.message("inspection.message.invalid.suffix.for.suffix.must.be.one", suffix, getDisplayName(literal.getNode()), validSuffixesStr.toString());
                } else {
                    message = RsBundle.message("inspection.message.with.suffix.invalid", getDisplayName(literal.getNode()));
                }

                holder.newAnnotation(HighlightSeverity.ERROR, message).create();
            }
        }

        // Check char literal length
        if (literal instanceof RsLiteralKind.CharLiteral) {
            RsLiteralKind.CharLiteral charLiteral = (RsLiteralKind.CharLiteral) literal;
            String value = charLiteral.getValue();
            String errorMessage = null;
            if (value == null || value.isEmpty()) {
                errorMessage = RsBundle.message("empty.0", getDisplayName(literal.getNode()));
            } else if (value.codePointCount(0, value.length()) > 1) {
                errorMessage = RsBundle.message("too.many.characters.in.0", getDisplayName(literal.getNode()));
            }
            if (errorMessage != null) {
                holder.newAnnotation(HighlightSeverity.ERROR, errorMessage).create();
            }
        }

        // Check delimiters
        if (literal instanceof RsLiteralKind.RsTextLiteral && ((RsLiteralKind.RsTextLiteral) literal).getHasUnpairedQuotes()) {
            holder.newAnnotation(HighlightSeverity.ERROR, RsBundle.message("inspection.message.unclosed", getDisplayName(literal.getNode()))).create();
        }
    }

    @NotNull
    private static String getDisplayName(@NotNull ASTNode node) {
        IElementType elementType = node.getElementType();
        if (elementType == RsElementTypes.INTEGER_LITERAL) return "integer literal";
        if (elementType == RsElementTypes.FLOAT_LITERAL) return "float literal";
        if (elementType == RsElementTypes.CHAR_LITERAL) return "char literal";
        if (elementType == RsElementTypes.BYTE_LITERAL) return "byte literal";
        if (elementType == RsElementTypes.STRING_LITERAL) return "string literal";
        if (elementType == RsElementTypes.BYTE_STRING_LITERAL) return "byte string literal";
        if (elementType == RsElementTypes.CSTRING_LITERAL) return "C string literal";
        if (elementType == RsElementTypes.RAW_STRING_LITERAL) return "raw string literal";
        if (elementType == RsElementTypes.RAW_BYTE_STRING_LITERAL) return "raw byte string literal";
        if (elementType == RsElementTypes.RAW_CSTRING_LITERAL) return "raw C string literal";
        return node.toString();
    }
}
