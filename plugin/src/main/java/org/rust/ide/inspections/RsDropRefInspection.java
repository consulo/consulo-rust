/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import consulo.language.editor.inspection.LocalQuickFix;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.ide.fixes.RemoveRefFix;
import org.rust.lang.core.psi.RsCallExpr;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsPathExpr;
import org.rust.lang.core.psi.RsVisitor;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.TyReference;

import consulo.language.psi.PsiElement;

import java.util.List;
import consulo.annotation.component.ExtensionImpl;

/**
 * Checks for calls to std::mem::drop with a reference instead of an owned value. Analogue of Clippy's drop_ref.
 * Quick fix: Use the owned value as the argument.
 */
@ExtensionImpl
public class RsDropRefInspection extends RsLocalInspectionTool {

@Override
    public RsVisitor buildVisitor(@Nonnull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitCallExpr(@Nonnull RsCallExpr expr) {
                inspectExpr(expr, holder);
            }
        };
    }

    public void inspectExpr(@Nonnull RsCallExpr expr, @Nonnull RsProblemsHolder holder) {
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

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.rs.drop.ref.display.name"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("rust"));
    }
}
