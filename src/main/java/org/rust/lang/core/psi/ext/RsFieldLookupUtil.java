/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsDotExpr;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsFieldLookup;

public final class RsFieldLookupUtil {
    private RsFieldLookupUtil() {
    }

    public static boolean isAsync(@NotNull RsFieldLookup fieldLookup) {
        return "await".equals(fieldLookup.getText());
    }

    /**
     * Returns the parent {@link RsDotExpr} of the field lookup.
     */
    @NotNull
    public static RsDotExpr getParentDotExpr(@NotNull RsFieldLookup fieldLookup) {
        PsiElement parent = fieldLookup.getParent();
        if (parent instanceof RsDotExpr) {
            return (RsDotExpr) parent;
        }
        throw new IllegalStateException("RsFieldLookup should always be a child of RsDotExpr");
    }

    /**
     * Returns the receiver expression of the dot expression containing this field lookup.
     */
    @NotNull
    public static RsExpr getReceiver(@NotNull RsFieldLookup fieldLookup) {
        return getParentDotExpr(fieldLookup).getExpr();
    }
}
