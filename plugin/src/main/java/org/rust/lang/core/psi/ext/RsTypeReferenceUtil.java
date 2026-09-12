/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.ast.ASTNode;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.ide.presentation.RsPsiRendererUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.lang.core.types.Substitution;
import org.rust.lang.core.types.ty.Ty;
import org.rust.openapiext.AstExt;
import consulo.language.psi.PsiElement;

public final class RsTypeReferenceUtil {
    private RsTypeReferenceUtil() {}

    /**
     * Walks up parents that are still part of the enclosing type expression (skipping
     * {@link RsTypeArgumentList}/{@link RsPath}) and returns the outermost such
     */
    @Nonnull
    public static RsTypeReference getOwner(@Nonnull RsTypeReference typeRef) {
        RsTypeReference last = typeRef;
        for (ASTNode node : AstExt.ancestors(typeRef.getNode())) {
            consulo.language.psi.PsiElement psi = node.getPsi();
            if (psi instanceof RsTypeArgumentList || psi instanceof RsPath) {
                continue;
            }
            if (psi instanceof RsPathType || psi instanceof RsTupleType || psi instanceof RsRefLikeType
                || psi instanceof RsTypeReference) {
                last = (RsTypeReference) psi;
            } else {
                break;
            }
        }
        return last;
    }

    /** {@code val RsTypeReference.rawType}. */
    @Nullable
    public static Ty getRawType(@Nullable RsTypeReference typeRef) {
        if (typeRef == null) return null;
        return ExtensionsUtil.getRawType(typeRef);
    }

    /** {@code tailrec fun RsTypeReference.skipParens()}. */
    @Nonnull
    public static RsTypeReference skipParens(@Nonnull RsTypeReference typeRef) {
        RsTypeReference current = typeRef;
        while (current instanceof RsParenType) {
            RsTypeReference inner = ((RsParenType) current).getTypeReference();
            if (inner == null) return current;
            current = inner;
        }
        return current;
    }

    /** {@code fun RsTypeReference.substAndGetText(subst: Substitution)} — renders type with substitutions applied. */
    @Nonnull
    public static String substAndGetText(@Nonnull RsTypeReference typeRef, @Nullable Substitution subst) {
        return RsPsiRendererUtil.getStubOnlyText(typeRef, subst != null ? subst : Substitution.EMPTY);
    }
}
