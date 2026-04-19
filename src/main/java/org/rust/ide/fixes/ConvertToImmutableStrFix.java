/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsExpr;

public class ConvertToImmutableStrFix extends ConvertToStrFix {
    public ConvertToImmutableStrFix(@NotNull RsExpr expr) {
        super(expr, "&str", "as_str");
    }
}
