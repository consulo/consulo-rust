/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.navigation.goto_;

import com.intellij.codeInsight.navigation.actions.TypeDeclarationProvider;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.infer.TypeInference;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.*;
import org.rust.lang.core.psi.ext.RsSelfParameterUtil;
import org.rust.lang.core.psi.ext.RsTraitItemUtil;
import org.rust.lang.core.psi.ext.RsElement;

public class RsTypeDeclarationProvider implements TypeDeclarationProvider {

    @Override
    @Nullable
    public PsiElement[] getSymbolTypeDeclarations(@NotNull PsiElement element) {
        Ty type;
        if (element instanceof RsFunction) {
            RsRetType retType = ((RsFunction) element).getRetType();
            if (retType == null) return null;
            RsTypeReference typeRef = retType.getTypeReference();
            if (typeRef == null) return null;
            type = RsTypesUtil.getNormType(typeRef);
        } else if (element instanceof RsNamedFieldDecl) {
            RsTypeReference typeRef = ((RsNamedFieldDecl) element).getTypeReference();
            if (typeRef == null) return null;
            type = RsTypesUtil.getNormType(typeRef);
        } else if (element instanceof RsConstant) {
            RsTypeReference typeRef = ((RsConstant) element).getTypeReference();
            if (typeRef == null) return null;
            type = RsTypesUtil.getNormType(typeRef);
        } else if (element instanceof RsPatBinding) {
            type = RsTypesUtil.getType((RsPatBinding) element);
        } else if (element instanceof RsSelfParameter) {
            RsSelfParameter selfParam = (RsSelfParameter) element;
            RsFunction parentFunction = RsSelfParameterUtil.getParentFunction(selfParam);
            RsAbstractableOwner owner = RsAbstractableImplUtil.getOwner(parentFunction);
            if (owner instanceof RsAbstractableOwner.Trait) {
                type = ((RsAbstractableOwner.Trait) owner).getTrait().getDeclaredType();
            } else if (owner instanceof RsAbstractableOwner.Impl) {
                RsTypeReference typeRef = ((RsAbstractableOwner.Impl) owner).getImpl().getTypeReference();
                if (typeRef == null) return null;
                type = RsTypesUtil.getNormType(typeRef);
            } else {
                return null;
            }
        } else {
            return null;
        }

        if (type == null) return null;

        RsElement typeDeclaration = baseTypeDeclaration(type);
        if (typeDeclaration == null) return null;
        return new PsiElement[]{typeDeclaration};
    }

    @Nullable
    private static RsElement baseTypeDeclaration(@NotNull Ty ty) {
        while (true) {
            if (ty instanceof TyAdt) {
                return ((TyAdt) ty).getItem();
            } else if (ty instanceof TyTraitObject) {
                var traits = ((TyTraitObject) ty).getTraits();
                if (traits.isEmpty()) return null;
                return traits.get(0).getTypedElement();
            } else if (ty instanceof TyTypeParameter) {
                TyTypeParameter.TypeParameter parameter = ((TyTypeParameter) ty).getParameter();
                if (parameter instanceof TyTypeParameter.Named) {
                    return ((TyTypeParameter.Named) parameter).getParameter();
                }
                // TODO: support self type parameter
                return null;
            } else if (ty instanceof TyProjection) {
                return ((TyProjection) ty).getTarget().getTypedElement();
            } else if (ty instanceof TyReference) {
                ty = ((TyReference) ty).getReferenced();
            } else if (ty instanceof TyPointer) {
                ty = ((TyPointer) ty).getReferenced();
            } else if (ty instanceof TyArray) {
                ty = ((TyArray) ty).getBase();
            } else if (ty instanceof TySlice) {
                ty = ((TySlice) ty).getElementType();
            } else if (ty instanceof TyAnon) {
                var traits = ((TyAnon) ty).getTraits();
                if (traits.isEmpty()) return null;
                return traits.get(0).getTypedElement();
            } else {
                return null;
            }
        }
    }
}
