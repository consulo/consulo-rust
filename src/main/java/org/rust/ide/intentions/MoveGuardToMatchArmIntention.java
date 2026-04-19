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
import org.rust.ide.utils.PsiModificationUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.PsiElementExt;
import org.rust.lang.core.psi.ext.RsMatchArmGuardUtil;
import org.rust.openapiext.EditorExt;

public class MoveGuardToMatchArmIntention extends RsElementBaseIntentionAction<MoveGuardToMatchArmIntention.Context> {
    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.move.guard.inside.match.arm");
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

    public static class Context {
        private final RsMatchArmGuard myGuard;
        private final RsExpr myGuardExpr;
        private final RsExpr myArmBody;

        public Context(@NotNull RsMatchArmGuard guard, @NotNull RsExpr guardExpr, @NotNull RsExpr armBody) {
            myGuard = guard;
            myGuardExpr = guardExpr;
            myArmBody = armBody;
        }

        @NotNull
        public RsMatchArmGuard getGuard() {
            return myGuard;
        }

        @NotNull
        public RsExpr getGuardExpr() {
            return myGuardExpr;
        }

        @NotNull
        public RsExpr getArmBody() {
            return myArmBody;
        }
    }

    @Nullable
    @Override
    public Context findApplicableContext(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) {
        RsMatchArmGuard guard = PsiElementExt.ancestorStrict(element, RsMatchArmGuard.class);
        if (guard == null) return null;
        RsExpr guardExprRaw = guard.getExpr();
        if (guardExprRaw != null && PsiElementExt.descendantOfTypeOrSelf(guardExprRaw, RsLetExpr.class) != null) return null;
        RsExpr guardExpr = guard.getExpr();
        if (guardExpr == null) return null;
        RsMatchArm parentArm = RsMatchArmGuardUtil.getParentMatchArm(guard);
        RsExpr armBody = parentArm.getExpr();
        if (armBody == null) return null;
        if (!PsiModificationUtil.canReplaceAll(guard, armBody)) return null;
        return new Context(guard, guardExpr, armBody);
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull Context ctx) {
        RsMatchArmGuard guard = ctx.getGuard();
        RsExpr guardExpr = ctx.getGuardExpr();
        RsExpr oldArmBody = ctx.getArmBody();
        int caretOffsetInGuard = editor.getCaretModel().getOffset() - guard.getTextOffset();
        RsPsiFactory psiFactory = new RsPsiFactory(project);
        RsIfExpr newArmBody = psiFactory.createIfExpression(guardExpr, oldArmBody);
        newArmBody = (RsIfExpr) oldArmBody.replace(newArmBody);
        guard.delete();
        EditorExt.moveCaretToOffset(editor, newArmBody, newArmBody.getTextOffset() + caretOffsetInGuard);
    }
}
