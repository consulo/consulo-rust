/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsTypeAlias;

public final class RsTypeAliasUtil {
    private RsTypeAliasUtil() {
    }

    @Nullable
    public static PsiElement getDefault(@NotNull RsTypeAlias typeAlias) {
        ASTNode child = typeAlias.getNode().findChildByType(RsElementTypes.DEFAULT);
        return child != null ? child.getPsi() : null;
    }
}
