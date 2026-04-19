/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
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

    @NotNull
    public static List<RsFieldDecl> getFields(@NotNull RsFieldsOwner owner) {
        return RsFieldsOwnerExtUtil.getFields(owner);
    }

    @NotNull
    public static List<RsNamedFieldDecl> getNamedFields(@NotNull RsFieldsOwner owner) {
        return RsFieldsOwnerExtUtil.getNamedFields(owner);
    }

    @NotNull
    public static List<? extends RsFieldDecl> getPositionalFields(@NotNull RsFieldsOwner owner) {
        return RsFieldsOwnerExtUtil.getPositionalFields(owner);
    }

    @NotNull
    public static List<Ty> getFieldTypes(@NotNull RsFieldsOwner owner) {
        return RsFieldsOwnerExtUtil.getFieldTypes(owner);
    }

    public static boolean isFieldless(@NotNull RsFieldsOwner owner) {
        return RsFieldsOwnerExtUtil.isFieldless(owner);
    }

    public static int getSize(@NotNull RsFieldsOwner owner) {
        return RsFieldsOwnerExtUtil.getSize(owner);
    }

    public static boolean canBeInstantiatedIn(@NotNull RsFieldsOwner owner, @NotNull RsMod mod) {
        return RsFieldsOwnerExtUtil.canBeInstantiatedIn(owner, mod);
    }

    @NotNull
    public static List<PsiReference> searchReferencesWithSelf(@NotNull RsElement element) {
        List<PsiReference> result = new java.util.ArrayList<>();
        for (PsiReference ref : com.intellij.psi.search.searches.ReferencesSearch.search((com.intellij.psi.PsiElement) element)) {
            result.add(ref);
        }
        return result;
    }
}
