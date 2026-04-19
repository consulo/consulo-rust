/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsExpr;

public class ConvertToMutStrFix extends ConvertToStrFix {
    public ConvertToMutStrFix(@NotNull RsExpr expr) {
        super(expr, "&mut str", "as_mut_str");
    }
}
