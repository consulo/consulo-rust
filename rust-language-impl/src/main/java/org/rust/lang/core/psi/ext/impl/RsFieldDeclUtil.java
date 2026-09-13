/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import org.rust.lang.core.psi.ext.impl.PsiElementUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.types.ty.Ty;

import java.util.List;
import org.rust.lang.core.psi.impl.RsPsiUtilUtil;
import org.rust.lang.core.psi.ext.*;

public final class RsFieldDeclUtil {
    private RsFieldDeclUtil() {
    }

    @Nullable
    public static RsFieldsOwner getOwner(@Nonnull RsFieldDecl fieldDecl) {
        return PsiElementUtil.stubAncestorStrict(fieldDecl, RsFieldsOwner.class);
    }

    @Nullable
    public static String getEscapedName(@Nonnull RsFieldDecl fieldDecl) {
        if (fieldDecl instanceof RsNamedElement) {
            String name = ((RsNamedElement) fieldDecl).getName();
            return name != null ? org.rust.lang.core.psi.impl.RsPsiUtilUtil.escapeIdentifierIfNeeded(name) : null;
        }
        return fieldDecl.getName();
    }

    /**
     * Returns the field types for the given fields owner, filtering cfg-disabled fields.
     */
    @Nonnull
    public static List<Ty> getFieldTypes(@Nonnull RsFieldsOwner owner) {
        return RsFieldsOwnerExtUtil.getFieldTypes(owner);
    }
}
