/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.extapi.psi.StubBasedPsiElementBase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsConstant;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.stubs.RsConstantStub;
import org.rust.lang.core.types.ty.Mutability;

/**
 * Extension functions for {@link RsConstant}.
 */
public final class RsConstantUtil {

    private RsConstantUtil() {
    }

    @Nullable
    private static RsConstantStub getStub(@NotNull RsConstant constant) {
        if (constant instanceof StubBasedPsiElementBase) {
            StubElement<?> stub = ((StubBasedPsiElementBase<?>) constant).getGreenStub();
            if (stub instanceof RsConstantStub) {
                return (RsConstantStub) stub;
            }
        }
        return null;
    }

    public static boolean isMut(@NotNull RsConstant constant) {
        RsConstantStub stub = getStub(constant);
        if (stub != null) return stub.isMut();
        return constant.getMut() != null;
    }

    public static boolean isConst(@NotNull RsConstant constant) {
        RsConstantStub stub = getStub(constant);
        if (stub != null) return stub.isConst();
        return constant.getConst() != null;
    }

    @NotNull
    public static RsConstantKind getKind(@NotNull RsConstant constant) {
        if (isMut(constant)) return RsConstantKind.MUT_STATIC;
        if (isConst(constant)) return RsConstantKind.CONST;
        return RsConstantKind.STATIC;
    }

    @Nullable
    public static PsiElement getDefault(@NotNull RsConstant constant) {
        com.intellij.lang.ASTNode child = constant.getNode().findChildByType(RsElementTypes.DEFAULT);
        return child != null ? child.getPsi() : null;
    }

    @NotNull
    public static Mutability getMutability(@NotNull RsConstant constant) {
        return Mutability.valueOf(isMut(constant));
    }

    @NotNull
    public static PsiElement getNameLikeElement(@NotNull RsConstant constant) {
        PsiElement nameId = constant.getNameIdentifier();
        if (nameId != null) return nameId;
        PsiElement underscore = constant.getUnderscore();
        if (underscore != null) return underscore;
        throw new IllegalStateException("Constant without name: `" + constant.getText() + "`");
    }
}
