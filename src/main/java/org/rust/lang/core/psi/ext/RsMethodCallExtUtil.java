/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;

public final class RsMethodCallExtUtil {
    private RsMethodCallExtUtil() {}

    /** {@code val RsMethodOrField.parentDotExpr: RsDotExpr get() = parent as RsDotExpr}. */
    @Nullable
    public static RsDotExpr getParentDotExpr(@NotNull RsMethodCall methodCall) {
        PsiElement parent = methodCall.getParent();
        return parent instanceof RsDotExpr ? (RsDotExpr) parent : null;
    }

    /** {@code val RsMethodOrField.receiver: RsExpr get() = parentDotExpr.expr}. */
    @Nullable
    public static RsExpr getReceiver(@NotNull RsMethodCall methodCall) {
        RsDotExpr dot = getParentDotExpr(methodCall);
        return dot != null ? dot.getExpr() : null;
    }

    /**
     * {@code val RsMethodCall.textRangeWithoutValueArguments: TextRange
     *  = TextRange(startOffset, typeArgumentList?.endOffset ?: identifier.endOffset)}.
     */
    @NotNull
    public static TextRange getTextRangeWithoutValueArguments(@NotNull RsMethodCall call) {
        int start = call.getTextRange().getStartOffset();
        RsTypeArgumentList typeArgs = call.getTypeArgumentList();
        int end = typeArgs != null
            ? typeArgs.getTextRange().getEndOffset()
            : call.getIdentifier().getTextRange().getEndOffset();
        return new TextRange(start, end);
    }
}
