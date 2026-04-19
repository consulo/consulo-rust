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
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.TyReference;
import org.rust.lang.core.psi.ext.RsSelfParameterUtil;
import org.rust.lang.core.psi.ext.RsPatBindingUtil;
import org.rust.lang.core.psi.ext.RsTypeReferenceUtil;
import org.rust.lang.core.psi.ext.RsElement;

public class AddMutableFix extends RsQuickFixBase<RsNamedElement> {
    @IntentionName
    private final String _text;
    public final boolean mutable = true;

    public AddMutableFix(@NotNull RsNamedElement binding) {
        super(binding);
        String name = binding.getName();
        _text = RsBundle.message("intention.name.make.mutable", name != null ? name : "");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.make.mutable");
    }

    @NotNull
    @Override
    public String getText() {
        return _text;
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsNamedElement element) {
        updateMutable(project, element, mutable);
    }

    @Nullable
    public static AddMutableFix createIfCompatible(@NotNull RsExpr expr) {
        RsElement declaration = RsTypesUtil.getDeclaration(expr);
        if (!(declaration instanceof RsNamedElement)) return null;
        RsNamedElement namedDecl = (RsNamedElement) declaration;

        if (namedDecl instanceof RsSelfParameter) {
            return new AddMutableFix(namedDecl);
        }

        if (namedDecl instanceof RsPatBinding) {
            RsPatBinding patBinding = (RsPatBinding) namedDecl;
            if (RsPatBindingUtil.getKind(patBinding) instanceof RsBindingModeKind.BindByValue
                && (RsPatBindingUtil.isArg(patBinding) || !(RsTypesUtil.getType(expr) instanceof TyReference))) {
                return new AddMutableFix(namedDecl);
            }
        }

        return null;
    }

    public static void updateMutable(@NotNull Project project, @NotNull RsNamedElement binding, boolean mutable) {
        if (binding instanceof RsPatBinding) {
            RsPatBinding patBinding = (RsPatBinding) binding;
            RsValueParameter parameter = RsPsiJavaUtil.ancestorStrict(patBinding, RsValueParameter.class);
            if (parameter != null) {
                RsTypeReference type = parameter.getTypeReference();
                if (type != null) {
                    type = RsTypeReferenceUtil.skipParens(type);
                    if (type instanceof RsRefLikeType) {
                        RsRefLikeType refType = (RsRefLikeType) type;
                        RsTypeReference innerTypeRef = refType.getTypeReference();
                        if (innerTypeRef == null) return;
                        RsValueParameter newParam = new RsPsiFactory(project)
                            .createValueParameter(parameter.getPat().getText(), innerTypeRef, mutable, refType.getLifetime());
                        parameter.replace(newParam);
                        return;
                    }
                }
            }
            boolean isRef = RsPatBindingUtil.getKind(patBinding) instanceof RsBindingModeKind.BindByReference;
            RsPatBinding newPatBinding = new RsPsiFactory(project).createPatBinding(patBinding.getIdentifier().getText(), mutable, isRef);
            patBinding.replace(newPatBinding);
        } else if (binding instanceof RsSelfParameter) {
            RsSelfParameter self = (RsSelfParameter) binding;
            RsSelfParameter newSelf;
            if (RsSelfParameterUtil.isRef(self)) {
                newSelf = new RsPsiFactory(project).createSelfReference(true);
            } else {
                newSelf = new RsPsiFactory(project).createSelf(true);
            }
            self.replace(newSelf);
        }
    }
}
