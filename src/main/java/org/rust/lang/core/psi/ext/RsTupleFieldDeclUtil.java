/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsTupleFieldDecl;

import java.util.List;

public final class RsTupleFieldDeclUtil {
    private RsTupleFieldDeclUtil() {
    }

    @Nullable
    public static Integer getPosition(@NotNull RsTupleFieldDecl tupleFieldDecl) {
        RsFieldsOwner owner = RsFieldDeclUtil.getOwner(tupleFieldDecl);
        if (owner == null) return null;
        List<RsTupleFieldDecl> fields = RsFieldsOwnerExtUtil.getPositionalFields(owner);
        for (int i = 0; i < fields.size(); i++) {
            if (fields.get(i) == tupleFieldDecl) {
                return i;
            }
        }
        return null;
    }
}
