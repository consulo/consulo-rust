/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.ide.fixes.SubstituteTextFix;
import org.rust.lang.core.CompilerFeature;
import org.rust.lang.core.FeatureAvailability;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsPatUtil;
import org.rust.lang.core.psi.ext.PsiElementExt;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.rust.lang.core.psi.ext.RsLetExprUtil;

/**
 * Detects redundant {@code else} statements preceded by an irrefutable pattern.
 * Quick fix: Remove {@code else}
 *
 * See also {@link RsConstantConditionIfInspection}.
 */
public class RsRedundantElseInspection extends RsLocalInspectionTool {

    @Override
    public String getDisplayName() {
        return RsBundle.message("redundant.else");
    }

    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitElseBranch(@NotNull RsElseBranch expr) {
                if (isElseBranchRedundant(expr)) {
                    registerProblem(holder, expr, expr.getElse().getTextRangeInParent());
                }
            }

            @Override
            public void visitLetElseBranch(@NotNull RsLetElseBranch expr) {
                if (CompilerFeature.getLET_ELSE().availability(expr) != FeatureAvailability.AVAILABLE) return;
                if (isLetElseBranchRedundant(expr)) {
                    registerProblem(holder, expr, expr.getElse().getTextRangeInParent());
                }
            }
        };
    }

    private static void registerProblem(@NotNull RsProblemsHolder holder, @NotNull RsElement expr, @NotNull TextRange textRange) {
        holder.registerProblem(
            expr,
            textRange,
            RsBundle.message("inspection.message.redundant.else"),
            SubstituteTextFix.delete(
                RsBundle.message("intention.name.remove.else"),
                expr.getContainingFile(),
                PsiElementExt.getRangeWithPrevSpace(expr)
            )
        );
    }

    private static boolean isElseBranchRedundant(@NotNull RsElseBranch elseBranch) {
        Set<RsCondition> conditions = new HashSet<>();
        PsiElement candidate = elseBranch;

        while (candidate instanceof RsElseBranch || candidate instanceof RsIfExpr) {
            PsiElement prev = candidate.getParent() != null ? candidate : null;
            // Collect left sibling conditions
            if (candidate instanceof RsIfExpr) {
                RsCondition condition = ((RsIfExpr) candidate).getCondition();
                if (condition != null) {
                    conditions.add(condition);
                }
            }
            // Walk up
            PsiElement sibling = candidate.getPrevSibling();
            while (sibling != null) {
                if (sibling instanceof RsCondition) {
                    conditions.add((RsCondition) sibling);
                }
                sibling = sibling.getPrevSibling();
            }
            candidate = candidate.getParent();
        }

        for (RsCondition condition : conditions) {
            if (isConditionRedundant(condition)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isLetElseBranchRedundant(@NotNull RsLetElseBranch letElseBranch) {
        PsiElement parent = letElseBranch.getParent();
        if (parent instanceof RsLetDecl) {
            RsPat pat = ((RsLetDecl) parent).getPat();
            return pat != null && RsPatUtil.isIrrefutable(pat);
        }
        return false;
    }

    private static boolean isConditionRedundant(@NotNull RsCondition condition) {
        RsExpr expr = condition.getExpr();
        if (!(expr instanceof RsLetExpr)) return false;
        List<RsPat> patList = RsLetExprUtil.getPatList((RsLetExpr) expr);
        for (RsPat pat : patList) {
            if (!RsPatUtil.isIrrefutable(pat)) return false;
        }
        return true;
    }
}
