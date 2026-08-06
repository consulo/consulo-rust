/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.document.util.TextRange;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsDotExpr;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsMethodCall;
import org.rust.lang.core.psi.RsTypeArgumentList;

public final class RsMethodCallUtil {
    private RsMethodCallUtil() {
    }

    @Nonnull
    public static TextRange getTextRangeWithoutValueArguments(@Nonnull RsMethodCall methodCall) {
        RsTypeArgumentList typeArgList = methodCall.getTypeArgumentList();
        int endOffset = typeArgList != null ? typeArgList.getTextRange().getEndOffset() : methodCall.getIdentifier().getTextRange().getEndOffset();
        return new TextRange(methodCall.getTextRange().getStartOffset(), endOffset);
    }

    @Nonnull
    public static RsDotExpr getParentDotExpr(@Nonnull RsMethodCall methodCall) {
        return (RsDotExpr) methodCall.getParent();
    }

    @Nonnull
    public static RsExpr getReceiver(@Nonnull RsMethodCall methodCall) {
        return getParentDotExpr(methodCall).getExpr();
    }
}
