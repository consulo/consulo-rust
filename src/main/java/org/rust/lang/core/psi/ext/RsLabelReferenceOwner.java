/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsLabel;

public interface RsLabelReferenceOwner extends RsElement {
    /**
     * Returns {@code break} in case of {@code RsBreakExpr} and {@code continue} in case of {@code RsContExpr}.
     */
    @NotNull
    PsiElement getOperator();

    @Nullable
    RsLabel getLabel();
}
