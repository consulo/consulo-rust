/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import consulo.language.editor.inspection.FileModifier.SafeFieldForPreview;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.api.workspace.PackageOrigin;
import org.rust.lang.core.presentation.TypeRendering;
import org.rust.lang.core.imports.RsImportHelper;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyUnit;
import org.rust.lang.core.types.ty.TyUnknown;

import java.util.Set;
import org.rust.lang.core.psi.ext.impl.RsFunctionUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsFunctionOrLambda;
import consulo.localize.LocalizeValue;
import org.rust.lang.core.imports.TypeReferencesInfo;
import org.rust.lang.core.psi.impl.*;
import org.rust.lang.core.psi.ext.impl.*;

public class ChangeReturnTypeFix extends RsQuickFixBase<RsElement> {
    @SafeFieldForPreview
    private final Ty myActualTy;
    
    private final String myText;

    public ChangeReturnTypeFix(@Nonnull RsElement element, @Nonnull Ty actualTy) {
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

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(myText);
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.change.return.type"));
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsElement element) {
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
        org.rust.lang.core.imports.TypeReferencesInfo info = RsImportHelper.getTypeReferencesInfoFromTys(owner, myActualTy, oldTy);
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
    private static RsFunctionOrLambda findCallableOwner(@Nonnull PsiElement element) {
        return RsPsiJavaUtil.contextStrict(element, RsFunctionOrLambda.class);
    }

    @Nullable
    public static ChangeReturnTypeFix createIfCompatible(@Nonnull RsElement element, @Nonnull Ty actualTy) {
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
