/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.intentions.util.macros.InvokeInside;
import org.rust.ide.utils.PsiModificationUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.PsiElementExt;
import org.rust.lang.core.psi.ext.RsMatchArmGuardUtil;
import org.rust.openapiext.EditorExt;
import consulo.localize.LocalizeValue;

public class MoveGuardToMatchArmIntention extends RsElementBaseIntentionAction<MoveGuardToMatchArmIntention.Context> {
    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.move.guard.inside.match.arm"));
        }

    @Nonnull
    public consulo.localize.LocalizeValue getFamilyName() {
        return getText();
        }

    @Nonnull
    @Override
    public InvokeInside getAttributeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    public static class Context {
        private final RsMatchArmGuard myGuard;
        private final RsExpr myGuardExpr;
        private final RsExpr myArmBody;

        public Context(@Nonnull RsMatchArmGuard guard, @Nonnull RsExpr guardExpr, @Nonnull RsExpr armBody) {
            myGuard = guard;
            myGuardExpr = guardExpr;
            myArmBody = armBody;
        }

        @Nonnull
        public RsMatchArmGuard getGuard() {
            return myGuard;
        }

        @Nonnull
        public RsExpr getGuardExpr() {
            return myGuardExpr;
        }

        @Nonnull
        public RsExpr getArmBody() {
            return myArmBody;
        }
    }

    @Nullable
    @Override
    public Context findApplicableContext(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiElement element) {
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
    public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull Context ctx) {
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
