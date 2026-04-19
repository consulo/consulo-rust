/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.codeInspection.util.IntentionName;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyInteger;

import java.util.List;
import org.rust.lang.core.psi.ext.RsDocAndAttributeOwnerUtil;
import org.rust.lang.core.psi.ext.RsElement;

public class ChangeReprAttributeFix extends RsQuickFixBase<RsElement> {
    @IntentionName
    private final String myText;
    private final String myActualTy;

    public ChangeReprAttributeFix(@NotNull RsElement element, @NotNull String enumName, @NotNull String actualTy) {
        super(element);
        this.myActualTy = actualTy;
        this.myText = RsBundle.message("intention.name.change.representation.enum.to.repr", enumName, actualTy);
    }

    @NotNull
    @Override
    public String getText() {
        return myText;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.change.repr.attribute");
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsElement element) {
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
                attrOwner.addBefore(newOuterAttribute, ((com.intellij.psi.PsiElement) attrOwner).getFirstChild());
                break;
            case 1:
                ((com.intellij.psi.PsiElement) reprAttributes.get(0)).replace(newOuterAttribute.getMetaItem());
                break;
            default:
                // multiple #[repr(...)] attributes are disallowed by "conflicting_repr_hints" hard lint
                return;
        }
    }

    @Nullable
    private static RsEnumItem findEnumOwner(@NotNull RsElement element) {
        if (element instanceof RsExpr && element.getContext() instanceof RsVariantDiscriminant) {
            return RsPsiJavaUtil.contextStrict(element, RsEnumItem.class);
        }
        return null;
    }

    @Nullable
    public static ChangeReprAttributeFix createIfCompatible(@NotNull RsElement element, @NotNull Ty actualTy) {
        if (RsElementUtil.getContainingCrate(element).getOrigin() != PackageOrigin.WORKSPACE) return null;
        if (!(actualTy instanceof TyInteger)) return null;
        RsEnumItem enumOwner = findEnumOwner(element);
        if (enumOwner == null) return null;
        String enumName = enumOwner.getName();
        if (enumName == null) enumName = "";
        return new ChangeReprAttributeFix(element, enumName, ((TyInteger) actualTy).getName());
    }
}
