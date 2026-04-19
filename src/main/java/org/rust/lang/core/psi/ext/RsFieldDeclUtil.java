/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.rust.lang.core.psi.ext.PsiElementUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.types.NormTypeUtil;
import org.rust.lang.core.types.ty.Ty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RsFieldDeclUtil {
    private RsFieldDeclUtil() {
    }

    @Nullable
    public static RsFieldsOwner getOwner(@NotNull RsFieldDecl fieldDecl) {
        return PsiElementUtil.stubAncestorStrict(fieldDecl, RsFieldsOwner.class);
    }

    @Nullable
    public static String getEscapedName(@NotNull RsFieldDecl fieldDecl) {
        if (fieldDecl instanceof RsNamedElement) {
            String name = ((RsNamedElement) fieldDecl).getName();
            return name != null ? org.rust.lang.core.psi.RsPsiUtilUtil.escapeIdentifierIfNeeded(name) : null;
        }
        return fieldDecl.getName();
    }

    /**
     * Returns the field types for the given fields owner, filtering cfg-disabled fields.
     */
    @NotNull
    public static List<Ty> getFieldTypes(@NotNull RsFieldsOwner owner) {
        return RsFieldsOwnerExtUtil.getFieldTypes(owner);
    }
}
