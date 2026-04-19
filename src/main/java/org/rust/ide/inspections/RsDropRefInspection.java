/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import com.intellij.codeInspection.LocalQuickFix;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.ide.fixes.RemoveRefFix;
import org.rust.lang.core.psi.RsCallExpr;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsPathExpr;
import org.rust.lang.core.psi.RsVisitor;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.TyReference;

import com.intellij.psi.PsiElement;

import java.util.List;

/**
 * Checks for calls to std::mem::drop with a reference instead of an owned value. Analogue of Clippy's drop_ref.
 * Quick fix: Use the owned value as the argument.
 */
public class RsDropRefInspection extends RsLocalInspectionTool {

    @Override
    public String getDisplayName() {
        return RsBundle.message("drop.reference");
    }

    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitCallExpr(@NotNull RsCallExpr expr) {
                inspectExpr(expr, holder);
            }
        };
    }

    public void inspectExpr(@NotNull RsCallExpr expr, @NotNull RsProblemsHolder holder) {
        RsExpr calleeExpr = expr.getExpr();
        if (!(calleeExpr instanceof RsPathExpr)) return;
        RsPathExpr pathExpr = (RsPathExpr) calleeExpr;

        PsiElement fn = pathExpr.getPath().getReference() != null ? pathExpr.getPath().getReference().resolve() : null;
        if (fn == null) return;
        if (fn != KnownItems.getKnownItems(expr).getDrop()) return;

        List<RsExpr> args = expr.getValueArgumentList().getExprList();
        if (args.size() != 1) return;
        RsExpr arg = args.get(0);

        if (RsTypesUtil.getType(arg) instanceof TyReference) {
            RemoveRefFix removeRefFix = RemoveRefFix.createIfCompatible(arg);
            LocalQuickFix[] fixes = removeRefFix != null ? new LocalQuickFix[]{removeRefFix} : LocalQuickFix.EMPTY_ARRAY;
            holder.registerProblem(
                expr,
                RsBundle.message("inspection.message.call.to.std.mem.drop.with.reference.argument.dropping.reference.does.nothing"),
                fixes);
        }
    }
}
