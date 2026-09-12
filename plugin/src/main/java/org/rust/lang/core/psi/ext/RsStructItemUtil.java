/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsStructItem;
import org.rust.lang.core.stubs.RsStructItemStub;
import org.rust.lang.core.types.RsPsiTypeImplUtil;
import org.rust.lang.core.types.ty.Ty;
import consulo.language.ast.ASTNode;

public final class RsStructItemUtil {
    private RsStructItemUtil() {
    }

    @Nullable
    public static PsiElement getUnion(@Nonnull RsStructItem structItem) {
        consulo.language.ast.ASTNode child = structItem.getNode().findChildByType(RsElementTypes.UNION);
        return child != null ? child.getPsi() : null;
    }

    @Nonnull
    public static RsStructKind getKind(@Nonnull RsStructItem structItem) {
        RsStructItemStub stub = RsPsiJavaUtil.getGreenStub(structItem);
        boolean hasUnion = stub != null ? stub.isUnion() : getUnion(structItem) != null;
        return hasUnion ? RsStructKind.UNION : RsStructKind.STRUCT;
    }

    public static boolean isTupleStruct(@Nonnull RsStructItem structItem) {
        return structItem.getTupleFields() != null;
    }

    @Nonnull
    public static Ty getDeclaredType(@Nonnull RsStructItem structItem) {
        return RsPsiTypeImplUtil.declaredType(structItem);
    }
}
