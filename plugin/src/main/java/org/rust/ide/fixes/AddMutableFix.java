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
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.TyReference;
import org.rust.lang.core.psi.ext.impl.RsSelfParameterUtil;
import org.rust.lang.core.psi.ext.impl.RsPatBindingUtil;
import org.rust.lang.core.psi.ext.impl.RsTypeReferenceUtil;
import org.rust.lang.core.psi.ext.RsElement;
import consulo.localize.LocalizeValue;
import org.rust.lang.core.psi.impl.*;
import org.rust.lang.core.psi.ext.impl.*;

public class AddMutableFix extends RsQuickFixBase<RsNamedElement> {
    
    private final String _text;
    public final boolean mutable = true;

    public AddMutableFix(@Nonnull RsNamedElement binding) {
        super(binding);
        String name = binding.getName();
        _text = RsBundle.message("intention.name.make.mutable", name != null ? name : "");
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.make.mutable"));
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(_text);
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsNamedElement element) {
        updateMutable(project, element, mutable);
    }

    @Nullable
    public static AddMutableFix createIfCompatible(@Nonnull RsExpr expr) {
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

    public static void updateMutable(@Nonnull Project project, @Nonnull RsNamedElement binding, boolean mutable) {
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
