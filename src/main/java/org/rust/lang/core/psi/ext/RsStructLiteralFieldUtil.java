/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsPatBinding;
import org.rust.lang.core.psi.RsStructLiteral;
import org.rust.lang.core.psi.RsStructLiteralField;

public final class RsStructLiteralFieldUtil {
    private RsStructLiteralFieldUtil() {
    }

    @NotNull
    public static RsStructLiteral getParentStructLiteral(@NotNull RsStructLiteralField field) {
        RsStructLiteral result = RsPsiJavaUtil.ancestorStrict(field, RsStructLiteral.class);
        assert result != null;
        return result;
    }

    @Nullable
    public static RsFieldDecl resolveToDeclaration(@NotNull RsStructLiteralField field) {
        return resolveToElement(field, RsFieldDecl.class);
    }

    @Nullable
    public static RsPatBinding resolveToBinding(@NotNull RsStructLiteralField field) {
        return resolveToElement(field, RsPatBinding.class);
    }

    @Nullable
    private static <T extends RsElement> T resolveToElement(@NotNull RsStructLiteralField field, @NotNull Class<T> clazz) {
        T result = null;
        for (com.intellij.psi.PsiElement element : field.getReference().multiResolve()) {
            if (clazz.isInstance(element)) {
                if (result != null) return null; // more than one
                result = clazz.cast(element);
            }
        }
        return result;
    }

    public static boolean isShorthand(@NotNull RsStructLiteralField field) {
        return field.getColon() == null;
    }
}
