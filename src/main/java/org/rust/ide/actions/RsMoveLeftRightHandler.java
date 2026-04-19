/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions;

import com.intellij.codeInsight.editorActions.moveLeftRight.MoveElementLeftRightHandler;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsArrayExprUtil;
import org.rust.lang.core.psi.ext.RsTypeParameterListUtil;

import java.util.Collections;
import java.util.List;

public class RsMoveLeftRightHandler extends MoveElementLeftRightHandler {

    @Override
    public PsiElement @NotNull [] getMovableSubElements(@NotNull PsiElement element) {
        List<? extends PsiElement> subElements;
        if (element instanceof RsArrayExpr arrayExpr) {
            List<RsExpr> elems = RsArrayExprUtil.getArrayElements(arrayExpr);
            subElements = elems != null ? elems : Collections.emptyList();
        } else if (element instanceof RsFormatMacroArgument arg) {
            subElements = arg.getFormatMacroArgList();
        } else if (element instanceof RsLifetimeParamBounds bounds) {
            subElements = bounds.getLifetimeList();
        } else if (element instanceof RsMetaItemArgs args) {
            List<PsiElement> combined = new java.util.ArrayList<>(args.getMetaItemList());
            combined.addAll(args.getLitExprList());
            subElements = combined;
        } else if (element instanceof RsTraitType traitType) {
            subElements = traitType.getPolyboundList();
        } else if (element instanceof RsTupleExpr tupleExpr) {
            subElements = tupleExpr.getExprList();
        } else if (element instanceof RsTupleType tupleType) {
            subElements = tupleType.getTypeReferenceList();
        } else if (element instanceof RsTupleFields tupleFields) {
            subElements = tupleFields.getTupleFieldDeclList();
        } else if (element instanceof RsTypeParamBounds bounds) {
            subElements = bounds.getPolyboundList();
        } else if (element instanceof RsTypeParameterList typeParamList) {
            subElements = RsTypeParameterListUtil.getGenericParameters(typeParamList);
        } else if (element instanceof RsUseGroup useGroup) {
            subElements = useGroup.getUseSpeckList();
        } else if (element instanceof RsValueArgumentList argList) {
            subElements = argList.getExprList();
        } else if (element instanceof RsValueParameterList paramList) {
            subElements = paramList.getValueParameterList();
        } else if (element instanceof RsVecMacroArgument vecMacro) {
            subElements = vecMacro.getSemicolon() == null ? vecMacro.getExprList() : Collections.emptyList();
        } else if (element instanceof RsWhereClause whereClause) {
            subElements = whereClause.getWherePredList();
        } else {
            return PsiElement.EMPTY_ARRAY;
        }
        return subElements.toArray(PsiElement.EMPTY_ARRAY);
    }
}
