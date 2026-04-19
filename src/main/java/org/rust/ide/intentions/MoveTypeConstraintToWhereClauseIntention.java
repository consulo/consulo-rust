/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.intentions.util.macros.InvokeInside;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.psi.PsiModificationUtils;

import java.util.List;
import org.rust.lang.core.psi.ext.RsFunctionUtil;

public class MoveTypeConstraintToWhereClauseIntention extends RsElementBaseIntentionAction<RsTypeParameterList> {
    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.move.type.constraint.to.where.clause");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @NotNull
    @Override
    public InvokeInside getAttributeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    @Nullable
    @Override
    public RsTypeParameterList findApplicableContext(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) {
        RsTypeParameterList genericParams = PsiElementExt.contextStrict(element, RsTypeParameterList.class);
        if (genericParams == null) return null;

        boolean hasTypeBounds = false;
        for (RsTypeParameter tp : genericParams.getTypeParameterList()) {
            if (tp.getTypeParamBounds() != null) {
                hasTypeBounds = true;
                break;
            }
        }
        boolean hasLifetimeBounds = false;
        for (RsLifetimeParameter lp : genericParams.getLifetimeParameterList()) {
            if (lp.getLifetimeParamBounds() != null) {
                hasLifetimeBounds = true;
                break;
            }
        }

        if (!hasTypeBounds && !hasLifetimeBounds) return null;
        return genericParams;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull RsTypeParameterList ctx) {
        List<RsLifetimeParameter> lifetimeBounds = ctx.getLifetimeParameterList();
        List<RsTypeParameter> typeBounds = ctx.getTypeParameterList();
        RsWhereClause whereClause = new RsPsiFactory(project).createWhereClause(lifetimeBounds, typeBounds);

        RsGenericDeclaration declaration = PsiElementExt.contextStrict(ctx, RsGenericDeclaration.class);
        if (declaration == null) return;
        PsiElement addedClause = addWhereClause(declaration, whereClause);
        if (addedClause == null) return;
        for (RsTypeParameter tp : typeBounds) {
            RsTypeParamBounds bounds = tp.getTypeParamBounds();
            if (bounds != null) bounds.delete();
        }
        for (RsLifetimeParameter lp : lifetimeBounds) {
            RsLifetimeParamBounds bounds = lp.getLifetimeParamBounds();
            if (bounds != null) bounds.delete();
        }
        org.rust.openapiext.Editor.moveCaretToOffset(editor, addedClause, PsiElementExt.getEndOffset(addedClause));
    }

    @Nullable
    private static PsiElement addWhereClause(@NotNull RsGenericDeclaration declaration, @NotNull RsWhereClause whereClause) {
        RsWhereClause existingWhereClause = declaration.getWhereClause();
        if (existingWhereClause != null) {
            List<RsWherePred> predList = existingWhereClause.getWherePredList();
            PsiModificationUtils.ensureTrailingComma(predList);
            List<RsWherePred> newPredList = whereClause.getWherePredList();
            if (!newPredList.isEmpty()) {
                existingWhereClause.addRangeAfter(
                    newPredList.get(0),
                    whereClause.getLastChild(),
                    existingWhereClause.getLastChild()
                );
            }
            return existingWhereClause;
        }

        PsiElement anchor = null;
        if (declaration instanceof RsTypeAlias) {
            anchor = ((RsTypeAlias) declaration).getEq();
        } else if (declaration instanceof RsTraitOrImpl) {
            anchor = ((RsTraitOrImpl) declaration).getMembers();
        } else if (declaration instanceof RsFunction) {
            anchor = RsFunctionUtil.getBlock((RsFunction) declaration);
        } else if (declaration instanceof RsStructItem) {
            anchor = ((RsStructItem) declaration).getSemicolon();
            if (anchor == null) {
                anchor = ((RsStructItem) declaration).getBlockFields();
            }
        } else if (declaration instanceof RsEnumItem) {
            anchor = ((RsEnumItem) declaration).getEnumBody();
        }
        if (anchor == null) return null;
        return ((PsiElement) declaration).addBefore(whereClause, anchor);
    }
}
