/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.*;

public final class RsMethodCallExtUtil {
    private RsMethodCallExtUtil() {}

    /** {@code val RsMethodOrField.parentDotExpr: RsDotExpr get() = parent as RsDotExpr}. */
    @Nullable
    public static RsDotExpr getParentDotExpr(@Nonnull RsMethodCall methodCall) {
        PsiElement parent = methodCall.getParent();
        return parent instanceof RsDotExpr ? (RsDotExpr) parent : null;
    }

    /** {@code val RsMethodOrField.receiver: RsExpr get() = parentDotExpr.expr}. */
    @Nullable
    public static RsExpr getReceiver(@Nonnull RsMethodCall methodCall) {
        RsDotExpr dot = getParentDotExpr(methodCall);
        return dot != null ? dot.getExpr() : null;
    }

    /**
     * {@code val RsMethodCall.textRangeWithoutValueArguments: TextRange
     *  = TextRange(startOffset, typeArgumentList?.endOffset ?: identifier.endOffset)}.
     */
    @Nonnull
    public static TextRange getTextRangeWithoutValueArguments(@Nonnull RsMethodCall call) {
        int start = call.getTextRange().getStartOffset();
        RsTypeArgumentList typeArgs = call.getTypeArgumentList();
        int end = typeArgs != null
            ? typeArgs.getTextRange().getEndOffset()
            : call.getIdentifier().getTextRange().getEndOffset();
        return new TextRange(start, end);
    }
}
