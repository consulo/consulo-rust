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
import org.rust.lang.core.psi.ext.RsTypeParameterListUtil;
import org.rust.lang.core.psi.ext.RsTypeReferenceUtil;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.RsTypeParameterUtil;
import org.rust.lang.core.psi.ext.RsLifetimeParameterUtil;

public class MoveTypeConstraintToParameterListIntention extends RsElementBaseIntentionAction<RsWhereClause> {
    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.move.type.constraint.to.parameter.list");
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
    public RsWhereClause findApplicableContext(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) {
        RsWhereClause whereClause = PsiElementExt.ancestorStrict(element, RsWhereClause.class);
        if (whereClause == null) return null;
        List<RsWherePred> wherePredList = whereClause.getWherePredList();
        if (wherePredList.isEmpty()) return null;

        RsGenericDeclaration declaration = PsiElementExt.ancestorStrict(whereClause, RsGenericDeclaration.class);
        if (declaration == null) return null;
        RsTypeParameterList typeParameterList = declaration.getTypeParameterList();
        if (typeParameterList == null) return null;
        List<RsLifetimeParameter> lifetimes = typeParameterList.getLifetimeParameterList();
        List<RsTypeParameter> types = typeParameterList.getTypeParameterList();

        for (RsWherePred pred : wherePredList) {
            boolean lifetimeMatch = pred.getLifetime() != null &&
                pred.getLifetime().getReference() != null &&
                lifetimes.contains(pred.getLifetime().getReference().resolve());
            RsTypeReference typeRef = pred.getTypeReference();
            RsTypeReference skipped = typeRef != null ? RsTypeReferenceUtil.skipParens(typeRef) : null;
            boolean typeMatch = false;
            if (skipped instanceof RsPathType) {
                RsPath path = ((RsPathType) skipped).getPath();
                if (path != null && path.getReference() != null) {
                    typeMatch = types.contains(path.getReference().resolve());
                }
            }
            if (!lifetimeMatch && !typeMatch) return null;
        }
        return whereClause;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull RsWhereClause ctx) {
        RsGenericDeclaration declaration = PsiElementExt.ancestorStrict(ctx, RsGenericDeclaration.class);
        if (declaration == null) return;

        RsTypeParameterList typeParameterList = declaration.getTypeParameterList();
        if (typeParameterList == null) return;

        List<RsGenericParameter> genericParameters = RsTypeParameterListUtil.getGenericParameters(typeParameterList);
        List<String> generics = new ArrayList<>();
        for (RsGenericParameter param : genericParameters) {
            if (param.getName() == null) continue;
            if (param instanceof RsTypeParameter) {
                generics.add(typeParameterText((RsTypeParameter) param));
            } else if (param instanceof RsLifetimeParameter) {
                generics.add(lifetimeParameterText((RsLifetimeParameter) param));
            } else if (param instanceof RsConstParameter) {
                generics.add(constParameterText((RsConstParameter) param));
            }
        }

        PsiElement newElement = new RsPsiFactory(project).createTypeParameterList(generics);
        PsiElement insertedParameterList = typeParameterList.replace(newElement);
        ctx.delete();
        org.rust.openapiext.Editor.moveCaretToOffset(editor, insertedParameterList, PsiElementExt.getEndOffset(insertedParameterList));
    }

    @NotNull
    private String typeParameterText(@NotNull RsTypeParameter param) {
        StringBuilder sb = new StringBuilder();
        sb.append(param.getName());
        List<RsPolybound> bounds = RsTypeParameterUtil.getBounds(param);
        Set<String> seen = new LinkedHashSet<>();
        for (RsPolybound bound : bounds) {
            seen.add(bound.getText());
        }
        List<String> distinctBounds = new ArrayList<>(seen);
        if (!distinctBounds.isEmpty()) {
            sb.append(":");
            for (int i = 0; i < distinctBounds.size(); i++) {
                if (i > 0) sb.append("+");
                sb.append(distinctBounds.get(i));
            }
        }
        RsTypeReference typeRef = param.getTypeReference();
        if (typeRef != null) {
            sb.append("=").append(typeRef.getText());
        }
        return sb.toString();
    }

    @NotNull
    private String lifetimeParameterText(@NotNull RsLifetimeParameter param) {
        StringBuilder sb = new StringBuilder();
        sb.append(param.getName());
        List<RsLifetime> bounds = RsLifetimeParameterUtil.getBounds(param);
        Set<String> seen = new LinkedHashSet<>();
        for (RsLifetime bound : bounds) {
            seen.add(bound.getText());
        }
        List<String> distinctBounds = new ArrayList<>(seen);
        if (!distinctBounds.isEmpty()) {
            sb.append(":");
            for (int i = 0; i < distinctBounds.size(); i++) {
                if (i > 0) sb.append("+");
                sb.append(distinctBounds.get(i));
            }
        }
        return sb.toString();
    }

    @NotNull
    private String constParameterText(@NotNull RsConstParameter param) {
        StringBuilder sb = new StringBuilder();
        sb.append("const ");
        sb.append(param.getName());
        RsTypeReference typeRef = param.getTypeReference();
        if (typeRef != null) {
            sb.append(": ").append(typeRef.getText());
        }
        return sb.toString();
    }
}
