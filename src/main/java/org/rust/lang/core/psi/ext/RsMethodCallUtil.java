/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsDotExpr;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsMethodCall;
import org.rust.lang.core.psi.RsTypeArgumentList;

public final class RsMethodCallUtil {
    private RsMethodCallUtil() {
    }

    @NotNull
    public static TextRange getTextRangeWithoutValueArguments(@NotNull RsMethodCall methodCall) {
        RsTypeArgumentList typeArgList = methodCall.getTypeArgumentList();
        int endOffset = typeArgList != null ? typeArgList.getTextRange().getEndOffset() : methodCall.getIdentifier().getTextRange().getEndOffset();
        return new TextRange(methodCall.getTextRange().getStartOffset(), endOffset);
    }

    @NotNull
    public static RsDotExpr getParentDotExpr(@NotNull RsMethodCall methodCall) {
        return (RsDotExpr) methodCall.getParent();
    }

    @NotNull
    public static RsExpr getReceiver(@NotNull RsMethodCall methodCall) {
        return getParentDotExpr(methodCall).getExpr();
    }
}
