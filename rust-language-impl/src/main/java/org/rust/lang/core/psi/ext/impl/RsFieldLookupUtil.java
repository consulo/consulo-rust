/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsDotExpr;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsFieldLookup;
import org.rust.lang.core.psi.ext.*;

public final class RsFieldLookupUtil {
    private RsFieldLookupUtil() {
    }

    public static boolean isAsync(@Nonnull RsFieldLookup fieldLookup) {
        return "await".equals(fieldLookup.getText());
    }

    /**
     * Returns the parent {@link RsDotExpr} of the field lookup.
     */
    @Nonnull
    public static RsDotExpr getParentDotExpr(@Nonnull RsFieldLookup fieldLookup) {
        PsiElement parent = fieldLookup.getParent();
        if (parent instanceof RsDotExpr) {
            return (RsDotExpr) parent;
        }
        throw new IllegalStateException("RsFieldLookup should always be a child of RsDotExpr");
    }

    /**
     * Returns the receiver expression of the dot expression containing this field lookup.
     */
    @Nonnull
    public static RsExpr getReceiver(@Nonnull RsFieldLookup fieldLookup) {
        return getParentDotExpr(fieldLookup).getExpr();
    }
}
