/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsLabelDecl;

public final class RsLabelDeclExtUtil {
    private RsLabelDeclExtUtil() {
    }

    /** Guaranteed by the grammar. */
    @NotNull
    public static RsLabeledExpression getOwner(@NotNull RsLabelDecl labelDecl) {
        return (RsLabeledExpression) labelDecl.getParent();
    }
}
