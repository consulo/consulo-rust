/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;

public final class RsTypeReferenceExtUtil {
    private RsTypeReferenceExtUtil() {
    }

    /**
     * Any type can be wrapped into parens, e.g. {@code let a: (i32) = 1;}.
     * Such type is parsed as {@link RsParenType}.
     * This method unwraps any number of parens around the type.
     */
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

    @Nonnull
    public static RsTypeReference getOwner(@Nonnull RsTypeReference typeRef) {
        PsiElement current = typeRef;
        RsTypeReference last = typeRef;
        while (current != null) {
            PsiElement parent = current.getParent();
            if (parent instanceof RsTypeArgumentList || parent instanceof RsPath) {
                current = parent;
                continue;
            }
            if (parent instanceof RsPathType || parent instanceof RsTupleType
                || parent instanceof RsRefLikeType || parent instanceof RsTypeReference) {
                if (parent instanceof RsTypeReference) {
                    last = (RsTypeReference) parent;
                }
                current = parent;
                continue;
            }
            break;
        }
        return last;
    }
}
