/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.injected;

import com.intellij.psi.LiteralTextEscaper;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.*;
import org.rust.lang.utils.RsEscapesUtils;
import org.rust.lang.core.psi.RsTokenType;

public final class RsStringLiteralEscaper {

    private RsStringLiteralEscaper() {
    }

    @NotNull
    public static LiteralTextEscaper<RsLitExpr> escaperForLiteral(@NotNull RsLitExpr lit) {
        com.intellij.lang.ASTNode child = lit.getNode().findChildByType(RsTokenType.RS_ALL_STRING_LITERALS);
        assert child != null : "`" + lit.getText() + "` is not a string literal";
        if (RsTokenType.RS_RAW_LITERALS.contains(child.getElementType())) {
            return new SimpleMultiLineTextEscaper(lit);
        } else {
            return new RsNormalStringLiteralEscaper(lit);
        }
    }

    private static class RsNormalStringLiteralEscaper extends LiteralTextEscaperBase<RsLitExpr> {
        RsNormalStringLiteralEscaper(@NotNull RsLitExpr host) {
            super(host);
        }

        @NotNull
        @Override
        protected com.intellij.openapi.util.Pair<int[], Boolean> parseStringCharacters(@NotNull String chars, @NotNull StringBuilder outChars) {
            RsEscapesUtils.ParseOffsetResult result = RsEscapesUtils.parseRustStringCharacters(chars, outChars);
            return new com.intellij.openapi.util.Pair<>(result.myOffsets, result.mySuccess);
        }

        @Override
        public boolean isOneLine() {
            return false;
        }
    }
}
