/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.codeInsight.intention.FileModifier.SafeFieldForPreview;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.psi.ext.RsSelfParameterUtil;

public class AddSelfFix extends RsQuickFixBase<RsFunction> {

    @SafeFieldForPreview
    private final SelfType selfType;
    private final String elementName;

    public AddSelfFix(@NotNull RsFunction function) {
        this(function, new SelfType.Reference(false));
    }

    public AddSelfFix(@NotNull RsFunction function, @NotNull SelfType selfType) {
        super(function);
        this.selfType = selfType;
        this.elementName = (function.getOwner() instanceof RsAbstractableOwner.Impl) ? "function" : "trait";
    }

    public static abstract class SelfType {
        public abstract RsSelfParameter createSelfPsiElement(RsPsiFactory psiFactory);

        public static class Pure extends SelfType {
            private final boolean mutable;
            public Pure(boolean mutable) { this.mutable = mutable; }
            @Override
            public RsSelfParameter createSelfPsiElement(RsPsiFactory psiFactory) {
                return psiFactory.createSelf(mutable);
            }
        }

        public static class Reference extends SelfType {
            private final boolean mutable;
            public Reference(boolean mutable) { this.mutable = mutable; }
            @Override
            public RsSelfParameter createSelfPsiElement(RsPsiFactory psiFactory) {
                return psiFactory.createSelfReference(mutable);
            }
        }

        public static class Adt extends SelfType {
            private final String typeText;
            public Adt(String typeText) { this.typeText = typeText; }
            @Override
            public RsSelfParameter createSelfPsiElement(RsPsiFactory psiFactory) {
                return psiFactory.createSelfWithType(typeText);
            }
        }

        public static SelfType fromSelf(RsSelfParameter self) {
            RsTypeReference selfTypeRef = self.getTypeReference();
            if (selfTypeRef != null) {
                return new Adt(selfTypeRef.getText());
            } else if (RsSelfParameterUtil.isRef(self)) {
                return new Reference(RsSelfParameterUtil.getMutability(self).isMut());
            } else {
                return new Pure(RsSelfParameterUtil.getMutability(self).isMut());
            }
        }
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.add.self.to", elementName);
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.family.name.add.self.to", elementName);
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsFunction element) {
        boolean hasParameters = !element.getRawValueParameters().isEmpty();
        RsPsiFactory psiFactory = new RsPsiFactory(project);

        RsValueParameterList valueParameterList = element.getValueParameterList();
        PsiElement lparen = valueParameterList != null ? valueParameterList.getFirstChild() : null;

        RsSelfParameter self = selfType.createSelfPsiElement(psiFactory);
        if (valueParameterList != null) {
            valueParameterList.addAfter(self, lparen);
        }

        if (hasParameters && lparen != null) {
            PsiElement parent = lparen.getParent();
            parent.addAfter(psiFactory.createComma(), parent.getFirstChild().getNextSibling());
        }
    }
}
