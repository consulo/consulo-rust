/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyInteger;

import java.util.List;
import org.rust.lang.core.psi.ext.RsDocAndAttributeOwnerUtil;
import org.rust.lang.core.psi.ext.RsElement;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import org.rust.lang.core.psi.RsMetaItem;

public class ChangeReprAttributeFix extends RsQuickFixBase<RsElement> {
    
    private final String myText;
    private final String myActualTy;

    public ChangeReprAttributeFix(@Nonnull RsElement element, @Nonnull String enumName, @Nonnull String actualTy) {
        super(element);
        this.myActualTy = actualTy;
        this.myText = RsBundle.message("intention.name.change.representation.enum.to.repr", enumName, actualTy);
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(myText);
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.change.repr.attribute"));
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsElement element) {
        RsEnumItem owner = findEnumOwner(element);
        if (!(owner instanceof RsDocAndAttributeOwner)) return;
        RsDocAndAttributeOwner attrOwner = (RsDocAndAttributeOwner) owner;
        List<org.rust.lang.core.psi.RsMetaItem> reprAttributes = new java.util.ArrayList<>();
        for (org.rust.lang.core.psi.RsMetaItem item : RsDocAndAttributeOwnerUtil.getQueryAttributes(attrOwner).getReprAttributes()) {
            reprAttributes.add(item);
        }
        RsOuterAttr newOuterAttribute = new RsPsiFactory(project).createOuterAttr("repr(" + myActualTy + ")");

        switch (reprAttributes.size()) {
            case 0:
                attrOwner.addBefore(newOuterAttribute, ((consulo.language.psi.PsiElement) attrOwner).getFirstChild());
                break;
            case 1:
                ((consulo.language.psi.PsiElement) reprAttributes.get(0)).replace(newOuterAttribute.getMetaItem());
                break;
            default:
                // multiple #[repr(...)] attributes are disallowed by "conflicting_repr_hints" hard lint
                return;
        }
    }

    @Nullable
    private static RsEnumItem findEnumOwner(@Nonnull RsElement element) {
        if (element instanceof RsExpr && element.getContext() instanceof RsVariantDiscriminant) {
            return RsPsiJavaUtil.contextStrict(element, RsEnumItem.class);
        }
        return null;
    }

    @Nullable
    public static ChangeReprAttributeFix createIfCompatible(@Nonnull RsElement element, @Nonnull Ty actualTy) {
        if (RsElementUtil.getContainingCrate(element).getOrigin() != PackageOrigin.WORKSPACE) return null;
        if (!(actualTy instanceof TyInteger)) return null;
        RsEnumItem enumOwner = findEnumOwner(element);
        if (enumOwner == null) return null;
        String enumName = enumOwner.getName();
        if (enumName == null) enumName = "";
        return new ChangeReprAttributeFix(element, enumName, ((TyInteger) actualTy).getName());
    }
}
