/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.fixes.SubstituteTextFix;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.PsiElementExt;

/**
 * Checks for potentially missing {@code else}s.
 * A partial analogue of Clippy's suspicious_else_formatting.
 * QuickFix: Change to {@code else if}
 */
public class RsMissingElseInspection extends RsLocalInspectionTool {

    @Override
    public String getDisplayName() {
        return RsBundle.message("missing.else");
    }

    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitExprStmt(@NotNull RsExprStmt expr) {
                RsIfExpr firstIf = extractIf(expr);
                if (firstIf == null) return;
                PsiElement sibling = expr.getNextSibling();
                // Skip whitespace/comments without newlines
                while (sibling != null && (sibling instanceof PsiWhiteSpace || sibling instanceof PsiComment) && !sibling.getText().contains("\n")) {
                    sibling = sibling.getNextSibling();
                }
                RsIfExpr nextIf = extractIf(sibling);
                if (nextIf == null) return;
                RsCondition condition = nextIf.getCondition();
                if (condition == null) return;
                RsExpr conditionExpr = condition.getExpr();
                if (conditionExpr == null) return;
                int rangeStart = expr.getStartOffsetInParent() + firstIf.getTextLength();
                int rangeLen = conditionExpr.getTextOffset() - firstIf.getTextOffset() - firstIf.getTextLength();
                holder.registerProblem(
                    expr.getParent(),
                    new TextRange(rangeStart, rangeStart + rangeLen),
                    RsBundle.message("inspection.message.suspicious.if.did.you.mean.else.if"),
                    SubstituteTextFix.insert(
                        RsBundle.message("intention.name.change.to.else.if"),
                        nextIf.getContainingFile(),
                        nextIf.getTextOffset(),
                        "else "
                    )
                );
            }
        };
    }

    @Nullable
    private static RsIfExpr extractIf(@Nullable PsiElement element) {
        if (element instanceof RsIfExpr) {
            return (RsIfExpr) element;
        } else if (element instanceof RsExprStmt) {
            return extractIf(((RsExprStmt) element).getFirstChild());
        }
        return null;
    }
}
