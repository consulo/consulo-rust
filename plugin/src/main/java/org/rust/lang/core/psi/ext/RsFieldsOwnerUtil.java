/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.psi.PsiReference;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsNamedFieldDecl;
import org.rust.lang.core.psi.RsTupleFieldDecl;
import org.rust.lang.core.types.ty.Ty;

import java.util.List;

/**
 * Delegates to {@link RsFieldsOwnerExtUtil} and {@link RsFieldsOwnerUtil}
 * for the actual implementations.
 */
public final class RsFieldsOwnerUtil {

    private RsFieldsOwnerUtil() {
    }

    @Nonnull
    public static List<RsFieldDecl> getFields(@Nonnull RsFieldsOwner owner) {
        return RsFieldsOwnerExtUtil.getFields(owner);
    }

    @Nonnull
    public static List<RsNamedFieldDecl> getNamedFields(@Nonnull RsFieldsOwner owner) {
        return RsFieldsOwnerExtUtil.getNamedFields(owner);
    }

    @Nonnull
    public static List<? extends RsFieldDecl> getPositionalFields(@Nonnull RsFieldsOwner owner) {
        return RsFieldsOwnerExtUtil.getPositionalFields(owner);
    }

    @Nonnull
    public static List<Ty> getFieldTypes(@Nonnull RsFieldsOwner owner) {
        return RsFieldsOwnerExtUtil.getFieldTypes(owner);
    }

    public static boolean isFieldless(@Nonnull RsFieldsOwner owner) {
        return RsFieldsOwnerExtUtil.isFieldless(owner);
    }

    public static int getSize(@Nonnull RsFieldsOwner owner) {
        return RsFieldsOwnerExtUtil.getSize(owner);
    }

    public static boolean canBeInstantiatedIn(@Nonnull RsFieldsOwner owner, @Nonnull RsMod mod) {
        return RsFieldsOwnerExtUtil.canBeInstantiatedIn(owner, mod);
    }

    @Nonnull
    public static List<PsiReference> searchReferencesWithSelf(@Nonnull RsElement element) {
        List<PsiReference> result = new java.util.ArrayList<>();
        for (PsiReference ref : consulo.language.psi.search.ReferencesSearch.search((consulo.language.psi.PsiElement) element)) {
            result.add(ref);
        }
        return result;
    }
}
