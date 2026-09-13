/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import consulo.language.impl.psi.stub.StubBasedPsiElementBase;
import consulo.language.psi.PsiElement;
import consulo.language.psi.stub.StubElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsConstant;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.stubs.RsConstantStub;
import org.rust.lang.core.types.ty.Mutability;
import consulo.language.ast.ASTNode;
import org.rust.lang.core.psi.ext.*;

/**
 * Extension functions for {@link RsConstant}.
 */
public final class RsConstantUtil {

    private RsConstantUtil() {
    }

    @Nullable
    private static RsConstantStub getStub(@Nonnull RsConstant constant) {
        if (constant instanceof StubBasedPsiElementBase) {
            StubElement<?> stub = ((StubBasedPsiElementBase<?>) constant).getGreenStub();
            if (stub instanceof RsConstantStub) {
                return (RsConstantStub) stub;
            }
        }
        return null;
    }

    public static boolean isMut(@Nonnull RsConstant constant) {
        RsConstantStub stub = getStub(constant);
        if (stub != null) return stub.isMut();
        return constant.getMut() != null;
    }

    public static boolean isConst(@Nonnull RsConstant constant) {
        RsConstantStub stub = getStub(constant);
        if (stub != null) return stub.isConst();
        return constant.getConst() != null;
    }

    @Nonnull
    public static RsConstantKind getKind(@Nonnull RsConstant constant) {
        if (isMut(constant)) return RsConstantKind.MUT_STATIC;
        if (isConst(constant)) return RsConstantKind.CONST;
        return RsConstantKind.STATIC;
    }

    @Nullable
    public static PsiElement getDefault(@Nonnull RsConstant constant) {
        consulo.language.ast.ASTNode child = constant.getNode().findChildByType(RsElementTypes.DEFAULT);
        return child != null ? child.getPsi() : null;
    }

    @Nonnull
    public static Mutability getMutability(@Nonnull RsConstant constant) {
        return Mutability.valueOf(isMut(constant));
    }

    @Nonnull
    public static PsiElement getNameLikeElement(@Nonnull RsConstant constant) {
        PsiElement nameId = constant.getNameIdentifier();
        if (nameId != null) return nameId;
        PsiElement underscore = constant.getUnderscore();
        if (underscore != null) return underscore;
        throw new IllegalStateException("Constant without name: `" + constant.getText() + "`");
    }
}
