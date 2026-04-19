/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.codeInsight.intention.FileModifier.SafeFieldForPreview;
import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.ide.presentation.TypeRendering;
import org.rust.ide.utils.imports.RsImportHelper;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyUnit;
import org.rust.lang.core.types.ty.TyUnknown;

import java.util.Set;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsFunctionOrLambda;

public class ChangeReturnTypeFix extends RsQuickFixBase<RsElement> {
    @SafeFieldForPreview
    private final Ty myActualTy;
    @IntentionName
    private final String myText;

    public ChangeReturnTypeFix(@NotNull RsElement element, @NotNull Ty actualTy) {
        super(element);
        this.myActualTy = actualTy;

        RsFunctionOrLambda callable = findCallableOwner(element);

        String item;
        String name;
        if (callable instanceof RsFunction) {
            RsFunction fn = (RsFunction) callable;
            item = RsFunctionUtil.getOwner(fn).isImplOrTrait() ? " of method" : " of function";
            String fnName = fn.getName();
            name = fnName != null ? " '" + fnName + "'" : "";
        } else if (callable instanceof RsLambdaExpr) {
            item = " of closure";
            name = "";
        } else {
            item = "";
            name = "";
        }

        Set<RsQualifiedNamedElement> useQualifiedName;
        if (callable != null) {
            useQualifiedName = RsImportHelper.getTypeReferencesInfoFromTys(callable, actualTy).getToQualify();
        } else {
            useQualifiedName = java.util.Collections.emptySet();
        }

        String rendered = TypeRendering.render(actualTy, element, useQualifiedName);
        this.myText = RsBundle.message("intention.name.change.return.type.to", item, name, rendered);
    }

    @NotNull
    @Override
    public String getText() {
        return myText;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.change.return.type");
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsElement element) {
        RsFunctionOrLambda owner = findCallableOwner(element);
        if (owner == null) return;
        RsRetType oldRetType = owner.getRetType();

        if (myActualTy instanceof TyUnit) {
            if (oldRetType != null) {
                oldRetType.delete();
            }
            return;
        }

        Ty oldTy = TyUnknown.INSTANCE;
        if (oldRetType != null && oldRetType.getTypeReference() != null) {
            oldTy = RsTypesUtil.getRawType(oldRetType.getTypeReference());
        }
        org.rust.ide.utils.imports.TypeReferencesInfo info = RsImportHelper.getTypeReferencesInfoFromTys(owner, myActualTy, oldTy);
        String text = TypeRendering.renderInsertionSafe(myActualTy, element, Integer.MAX_VALUE, info.getToQualify(), true, false, true, true);
        RsRetType retType = new RsPsiFactory(project).createRetType(text);

        if (oldRetType != null) {
            oldRetType.replace(retType);
        } else {
            owner.addAfter(retType, owner.getValueParameterList());
        }

        RsImportHelper.importTypeReferencesFromTy(owner, myActualTy);
    }

    @Nullable
    private static RsFunctionOrLambda findCallableOwner(@NotNull PsiElement element) {
        return RsPsiJavaUtil.contextStrict(element, RsFunctionOrLambda.class);
    }

    @Nullable
    public static ChangeReturnTypeFix createIfCompatible(@NotNull RsElement element, @NotNull Ty actualTy) {
        if (RsElementUtil.getContainingCrate(element).getOrigin() != PackageOrigin.WORKSPACE) return null;

        RsFunctionOrLambda owner = findCallableOwner(element);
        if (owner instanceof RsFunction && RsAbstractableUtil.getSuperItem((RsFunction) owner) != null) {
            return null; // TODO: Support overridden items
        }

        if (owner instanceof RsLambdaExpr && ((RsLambdaExpr) owner).getRetType() == null) {
            return null;
        }

        RsExpr retExpr;
        if (owner instanceof RsFunction) {
            RsBlock block = RsFunctionUtil.getBlock((RsFunction) owner);
            retExpr = block != null ? RsBlockUtil.getExpandedTailExpr(block) : null;
        } else if (owner instanceof RsLambdaExpr) {
            retExpr = ((RsLambdaExpr) owner).getExpr();
        } else {
            return null;
        }

        boolean isRetExpr = element.getParent() instanceof RsRetExpr || retExpr == element;
        if (!isRetExpr) return null;

        return new ChangeReturnTypeFix(element, actualTy);
    }
}
