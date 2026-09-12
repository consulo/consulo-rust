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
import org.rust.lang.core.psi.ext.RsBlockUtil;
import org.rust.lang.core.psi.ext.RsBlockExprUtil;
import org.rust.lang.core.types.ty.TyUnit;
import org.rust.lang.core.types.infer.TypeInference;
import org.rust.openapiext.EditorExt;
import org.rust.lang.core.psi.ext.RsStmtUtil;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import consulo.localize.LocalizeValue;

public class UnwrapSingleExprIntention extends RsElementBaseIntentionAction<UnwrapSingleExprIntention.Context> {
    @Nonnull
    public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.remove.braces.from.single.expression"));
        }

    @Nonnull
    @Override
    public InvokeInside getAttributeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    public static class Context {
        private final RsBlockExpr myBlockExpr;
        private final RsExpr myExpr;

        public Context(@Nonnull RsBlockExpr blockExpr, @Nonnull RsExpr expr) {
            myBlockExpr = blockExpr;
            myExpr = expr;
        }

        @Nonnull
        public RsBlockExpr getBlockExpr() {
            return myBlockExpr;
        }

        @Nonnull
        public RsExpr getExpr() {
            return myExpr;
        }
    }

    @Nullable
    @Override
    public Context findApplicableContext(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiElement element) {
        RsBlockExpr blockExpr = PsiElementExt.ancestorStrict(element, RsBlockExpr.class);
        if (blockExpr == null) return null;
        if (RsBlockExprUtil.isUnsafe(blockExpr) || RsBlockExprUtil.isAsync(blockExpr)
            || RsBlockExprUtil.isTry(blockExpr) || RsBlockExprUtil.isConst(blockExpr)) return null;
        RsBlock block = blockExpr.getBlock();

        RsExprStmt singleStatement = (RsExprStmt) RsBlockUtil.singleStmt(block);
        if (singleStatement == null) return null;
        if (!PsiModificationUtil.canReplace(blockExpr)) return null;

        if (RsStmtUtil.isTailStmt(singleStatement)) {
            setText(consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.remove.braces.from.single.expression")));
            return new Context(blockExpr, singleStatement.getExpr());
        } else if (RsTypesUtil.getType(singleStatement.getExpr()) instanceof TyUnit) {
            setText(consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.remove.braces.from.single.expression.statement")));
            return new Context(blockExpr, singleStatement.getExpr());
        }
        return null;
    }

    @Override
    public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull Context ctx) {
        PsiElement parent = ctx.getBlockExpr().getParent();
        if (parent instanceof RsMatchArm && ((RsMatchArm) parent).getComma() == null) {
            parent.add(new RsPsiFactory(project).createComma());
        }

        RsExpr element = ctx.getExpr();
        int caretOffset = editor.getCaretModel().getOffset();
        int relativeCaretPosition = Math.min(Math.max(caretOffset - element.getTextOffset(), 0), element.getTextLength());

        RsExpr insertedElement = (RsExpr) ctx.getBlockExpr().replace(element);
        EditorExt.moveCaretToOffset(editor, insertedElement, insertedElement.getTextOffset() + relativeCaretPosition);
    }
}
