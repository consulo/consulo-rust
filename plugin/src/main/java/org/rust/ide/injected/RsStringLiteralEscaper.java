/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.injected;

import consulo.language.psi.LiteralTextEscaper;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.*;
import org.rust.lang.utils.RsEscapesUtils;
import consulo.language.ast.ASTNode;
import consulo.util.lang.Pair;

public final class RsStringLiteralEscaper {

    private RsStringLiteralEscaper() {
    }

    @Nonnull
    public static LiteralTextEscaper<RsLitExpr> escaperForLiteral(@Nonnull RsLitExpr lit) {
        ASTNode child = lit.getNode().findChildByType(RsTokenSets.RS_ALL_STRING_LITERALS);
        assert child != null : "`" + lit.getText() + "` is not a string literal";
        if (RsTokenSets.RS_RAW_LITERALS.contains(child.getElementType())) {
            return new SimpleMultiLineTextEscaper(lit);
        } else {
            return new RsNormalStringLiteralEscaper(lit);
        }
    }

    private static class RsNormalStringLiteralEscaper extends LiteralTextEscaperBase<RsLitExpr> {
        RsNormalStringLiteralEscaper(@Nonnull RsLitExpr host) {
            super(host);
        }

        @Nonnull
        @Override
        protected consulo.util.lang.Pair<int[], Boolean> parseStringCharacters(@Nonnull String chars, @Nonnull StringBuilder outChars) {
            RsEscapesUtils.ParseOffsetResult result = RsEscapesUtils.parseRustStringCharacters(chars, outChars);
            return new consulo.util.lang.Pair<>(result.myOffsets, result.mySuccess);
        }

        @Override
        public boolean isOneLine() {
            return false;
        }
    }
}
