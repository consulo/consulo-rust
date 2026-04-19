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
import org.rust.lang.core.psi.RsTraitType;
import org.rust.lang.core.stubs.RsTraitTypeStub;

public final class RsTraitTypeExtUtil {
    private RsTraitTypeExtUtil() {
    }

    public static boolean isImpl(@NotNull RsTraitType traitType) {
        RsTraitTypeStub stub = RsPsiJavaUtil.getGreenStub(traitType);
        if (stub != null) return stub.isImpl();
        return traitType.getImpl() != null;
    }

    @Nullable
    public static PsiElement getDyn(@NotNull RsTraitType traitType) {
        ASTNode child = traitType.getNode().findChildByType(RsElementTypes.DYN);
        return child != null ? child.getPsi() : null;
    }
}
