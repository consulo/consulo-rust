/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsPatBinding;
import org.rust.lang.core.psi.RsStructLiteral;
import org.rust.lang.core.psi.RsStructLiteralField;

public final class RsStructLiteralFieldUtil {
    private RsStructLiteralFieldUtil() {
    }

    @Nonnull
    public static RsStructLiteral getParentStructLiteral(@Nonnull RsStructLiteralField field) {
        RsStructLiteral result = RsPsiJavaUtil.ancestorStrict(field, RsStructLiteral.class);
        assert result != null;
        return result;
    }

    @Nullable
    public static RsFieldDecl resolveToDeclaration(@Nonnull RsStructLiteralField field) {
        return resolveToElement(field, RsFieldDecl.class);
    }

    @Nullable
    public static RsPatBinding resolveToBinding(@Nonnull RsStructLiteralField field) {
        return resolveToElement(field, RsPatBinding.class);
    }

    @Nullable
    private static <T extends RsElement> T resolveToElement(@Nonnull RsStructLiteralField field, @Nonnull Class<T> clazz) {
        T result = null;
        for (consulo.language.psi.PsiElement element : field.getReference().multiResolve()) {
            if (clazz.isInstance(element)) {
                if (result != null) return null; // more than one
                result = clazz.cast(element);
            }
        }
        return result;
    }

    public static boolean isShorthand(@Nonnull RsStructLiteralField field) {
        return field.getColon() == null;
    }
}
