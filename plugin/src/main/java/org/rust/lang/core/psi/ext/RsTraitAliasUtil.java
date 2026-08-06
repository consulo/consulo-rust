/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsTraitAlias;

public final class RsTraitAliasUtil {
    private RsTraitAliasUtil() {
    }

    @Nullable
    public static PsiElement getDefault(@Nonnull RsTraitAlias traitAlias) {
        consulo.language.ast.ASTNode child = traitAlias.getNode().findChildByType(RsElementTypes.DEFAULT);
        return child != null ? child.getPsi() : null;
    }
}
