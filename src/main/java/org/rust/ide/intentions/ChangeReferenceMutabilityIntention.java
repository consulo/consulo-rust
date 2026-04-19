/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.rust.ide.intentions.util.macros.InvokeInside;
import org.rust.ide.utils.PsiModificationUtil;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.RsRefLikeType;
import org.rust.lang.core.psi.RsTypeReference;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;
import org.rust.lang.core.types.ty.Mutability;
import org.rust.lang.core.psi.ext.RsRefLikeTypeUtil;

public abstract class ChangeReferenceMutabilityIntention extends RsElementBaseIntentionAction<ChangeReferenceMutabilityIntention.Context> {

    @Override
    public String getFamilyName() {
        return getText();
    }

    @Override
    public InvokeInside getAttributeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    protected abstract Mutability getNewMutability();

    public static class Context {
        public final RsRefLikeType refType;
        public final RsTypeReference referencedType;

        public Context(RsRefLikeType refType, RsTypeReference referencedType) {
            this.refType = refType;
            this.referencedType = referencedType;
        }
    }

    @Override
    public Context findApplicableContext(Project project, Editor editor, PsiElement element) {
        RsRefLikeType refType = RsPsiJavaUtil.ancestorStrict(element, RsRefLikeType.class);
        if (refType == null) return null;
        if (!RsRefLikeTypeUtil.isRef(refType)) return null;
        RsTypeReference referencedType = refType.getTypeReference();
        if (referencedType == null) return null;

        if (RsRefLikeTypeUtil.getMutability(refType) == getNewMutability()) return null;
        if (!PsiModificationUtil.canReplace(refType)) return null;

        return new Context(refType, referencedType);
    }

    @Override
    public void invoke(Project project, Editor editor, Context ctx) {
        RsRefLikeType newType = new RsPsiFactory(project).createReferenceType(ctx.referencedType.getText(), getNewMutability());
        ctx.refType.replace(newType);
    }
}
