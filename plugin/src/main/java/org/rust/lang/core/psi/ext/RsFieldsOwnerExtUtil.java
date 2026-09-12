/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsBlockFields;
import org.rust.lang.core.psi.RsNamedFieldDecl;
import org.rust.lang.core.psi.RsTupleFieldDecl;
import org.rust.lang.core.psi.RsTupleFields;
import org.rust.lang.core.types.NormTypeUtil;
import org.rust.lang.core.types.ty.Ty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.rust.lang.core.psi.ext.RsVisibilityUtil;
import org.rust.lang.core.types.RsTypesUtil;

public final class RsFieldsOwnerExtUtil {
    private RsFieldsOwnerExtUtil() {
    }

    @Nonnull
    public static List<RsFieldDecl> getFields(@Nonnull RsFieldsOwner owner) {
        List<RsFieldDecl> result = new ArrayList<>();
        result.addAll(getNamedFields(owner));
        result.addAll(getPositionalFields(owner));
        return result;
    }

    /** Returns those named fields that are not disabled by cfg attributes. */
    @Nonnull
    public static List<RsNamedFieldDecl> getNamedFields(@Nonnull RsFieldsOwner owner) {
        RsBlockFields blockFields = owner.getBlockFields();
        if (blockFields == null) return Collections.emptyList();
        List<RsNamedFieldDecl> result = new ArrayList<>();
        for (RsNamedFieldDecl field : blockFields.getNamedFieldDeclList()) {
            if (RsDocAndAttributeOwnerUtil.existsAfterExpansionSelf(field, null)) {
                result.add(field);
            }
        }
        return result;
    }

    /** Returns those positional (tuple) fields that are not disabled by cfg attributes. */
    @Nonnull
    public static List<RsTupleFieldDecl> getPositionalFields(@Nonnull RsFieldsOwner owner) {
        RsTupleFields tupleFields = owner.getTupleFields();
        if (tupleFields == null) return Collections.emptyList();
        List<RsTupleFieldDecl> result = new ArrayList<>();
        for (RsTupleFieldDecl field : tupleFields.getTupleFieldDeclList()) {
            if (RsDocAndAttributeOwnerUtil.existsAfterExpansionSelf(field, null)) {
                result.add(field);
            }
        }
        return result;
    }

    /**
     * If some field of a struct is private (not visible from {@code mod}),
     * it isn't possible to instantiate it at {@code mod} anyhow.
     */
    public static boolean canBeInstantiatedIn(@Nonnull RsFieldsOwner owner, @Nonnull RsMod mod) {
        for (RsFieldDecl field : getFields(owner)) {
            if (!RsVisibilityUtil.isVisibleFrom(field, mod)) return false;
        }
        return true;
    }

    @Nonnull
    public static List<Ty> getFieldTypes(@Nonnull RsFieldsOwner owner) {
        List<Ty> result = new ArrayList<>();
        for (RsFieldDecl field : getFields(owner)) {
            if (RsDocAndAttributeOwnerUtil.existsAfterExpansionSelf(field, null)) {
                if (field.getTypeReference() != null) {
                    result.add(org.rust.lang.core.types.RsTypesUtil.getNormType(field.getTypeReference()));
                }
            }
        }
        return result;
    }

    /**
     * True for unit structs and unit enum variants (no fields at all):
     * {@code struct S;} or {@code enum E { A }},
     * but false for {@code struct S {}} or {@code struct S()}.
     */
    public static boolean isFieldless(@Nonnull RsFieldsOwner owner) {
        return owner.getBlockFields() == null && owner.getTupleFields() == null;
    }

    public static int getSize(@Nonnull RsFieldsOwner owner) {
        return getFields(owner).size();
    }
}
