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
import org.rust.lang.core.psi.ext.RsBlockUtil;
import org.rust.lang.core.psi.ext.RsBlockExprUtil;
import org.rust.lang.core.types.ty.TyUnit;
import org.rust.lang.core.types.infer.TypeInference;
import org.rust.openapiext.EditorExt;
import org.rust.lang.core.psi.ext.RsStmtUtil;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.psi.ext.RsFunctionUtil;

public class UnwrapSingleExprIntention extends RsElementBaseIntentionAction<UnwrapSingleExprIntention.Context> {
    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.name.remove.braces.from.single.expression");
    }

    @NotNull
    @Override
    public InvokeInside getAttributeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    public static class Context {
        private final RsBlockExpr myBlockExpr;
        private final RsExpr myExpr;

        public Context(@NotNull RsBlockExpr blockExpr, @NotNull RsExpr expr) {
            myBlockExpr = blockExpr;
            myExpr = expr;
        }

        @NotNull
        public RsBlockExpr getBlockExpr() {
            return myBlockExpr;
        }

        @NotNull
        public RsExpr getExpr() {
            return myExpr;
        }
    }

    @Nullable
    @Override
    public Context findApplicableContext(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) {
        RsBlockExpr blockExpr = PsiElementExt.ancestorStrict(element, RsBlockExpr.class);
        if (blockExpr == null) return null;
        if (RsBlockExprUtil.isUnsafe(blockExpr) || RsBlockExprUtil.isAsync(blockExpr)
            || RsBlockExprUtil.isTry(blockExpr) || RsBlockExprUtil.isConst(blockExpr)) return null;
        RsBlock block = blockExpr.getBlock();

        RsExprStmt singleStatement = (RsExprStmt) RsBlockUtil.singleStmt(block);
        if (singleStatement == null) return null;
        if (!PsiModificationUtil.canReplace(blockExpr)) return null;

        if (RsStmtUtil.isTailStmt(singleStatement)) {
            setText(RsBundle.message("intention.name.remove.braces.from.single.expression"));
            return new Context(blockExpr, singleStatement.getExpr());
        } else if (RsTypesUtil.getType(singleStatement.getExpr()) instanceof TyUnit) {
            setText(RsBundle.message("intention.name.remove.braces.from.single.expression.statement"));
            return new Context(blockExpr, singleStatement.getExpr());
        }
        return null;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull Context ctx) {
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
