/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsLabelDecl;
import org.rust.lang.core.psi.ext.*;

public final class RsLabelDeclExtUtil {
    private RsLabelDeclExtUtil() {
    }

    /** Guaranteed by the grammar. */
    @Nonnull
    public static RsLabeledExpression getOwner(@Nonnull RsLabelDecl labelDecl) {
        return (RsLabeledExpression) labelDecl.getParent();
    }
}
