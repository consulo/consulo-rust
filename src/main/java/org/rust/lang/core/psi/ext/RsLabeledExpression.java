/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsBlock;
import org.rust.lang.core.psi.RsLabelDecl;

public interface RsLabeledExpression extends RsElement {
    @Nullable
    RsLabelDecl getLabelDecl();

    @Nullable
    RsBlock getBlock();
}
