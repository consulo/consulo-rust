/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsStructItem;
import org.rust.lang.core.stubs.RsStructItemStub;
import org.rust.lang.core.types.RsPsiTypeImplUtil;
import org.rust.lang.core.types.ty.Ty;

public final class RsStructItemUtil {
    private RsStructItemUtil() {
    }

    @Nullable
    public static PsiElement getUnion(@NotNull RsStructItem structItem) {
        com.intellij.lang.ASTNode child = structItem.getNode().findChildByType(RsElementTypes.UNION);
        return child != null ? child.getPsi() : null;
    }

    @NotNull
    public static RsStructKind getKind(@NotNull RsStructItem structItem) {
        RsStructItemStub stub = RsPsiJavaUtil.getGreenStub(structItem);
        boolean hasUnion = stub != null ? stub.isUnion() : getUnion(structItem) != null;
        return hasUnion ? RsStructKind.UNION : RsStructKind.STRUCT;
    }

    public static boolean isTupleStruct(@NotNull RsStructItem structItem) {
        return structItem.getTupleFields() != null;
    }

    @NotNull
    public static Ty getDeclaredType(@NotNull RsStructItem structItem) {
        return RsPsiTypeImplUtil.declaredType(structItem);
    }
}
