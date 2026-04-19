/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.checkMatch;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.ide.fixes.SubstituteTextFix;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.ide.inspections.RsWithMacrosInspectionVisitor;
import org.rust.ide.inspections.lints.RsLint;
import org.rust.ide.inspections.lints.RsLintInspection;
import org.rust.ide.utils.checkMatch.*;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.infer.FoldUtil;
import org.rust.lang.core.types.infer.TypeInference;
import org.rust.lang.core.types.ty.TyUnknown;
import org.rust.lang.core.types.ty.Ty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.rust.lang.core.psi.ext.RsMatchArmUtil;

public class RsUnreachablePatternsInspection extends RsLintInspection {

    @NotNull
    @Override
    public String getDisplayName() {
        return RsBundle.message("unreachable.patterns");
    }

    @Override
    protected RsLint getLint(@NotNull PsiElement element) {
        return RsLint.UnreachablePattern;
    }

    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitMatchExpr(@NotNull RsMatchExpr matchExpr) {
                RsExpr expr = matchExpr.getExpr();
                if (expr == null) return;
                Ty exprType = RsTypesUtil.getType(expr);
                if (FoldUtil.containsTyOfClass(exprType, TyUnknown.class)) return;
                try {
                    checkUnreachablePatterns(matchExpr, holder);
                } catch (RuntimeException ignored) {
                }
            }
        };
    }

    private void checkUnreachablePatterns(@NotNull RsMatchExpr match, @NotNull RsProblemsHolder holder) {
        List<RsMatchArm> arms = RsMatchExprUtil.getArms(match);
        List<List<Pattern>> matrix = CheckMatchUtil.calculateMatrix(arms);
        if (matrix.isEmpty() || !CheckMatchUtil.isWellTyped(matrix)) return;

        List<RsPat> armPats = new ArrayList<>();
        for (RsMatchArm arm : arms) {
            armPats.addAll(RsMatchArmUtil.getPatList(arm));
        }

        List<List<Pattern>> seen = new ArrayList<>();

        for (int i = 0; i < matrix.size(); i++) {
            List<Pattern> patterns = matrix.get(i);
            RsPat armPat = armPats.get(i);
            RsFile crateRoot = (RsFile) RsElementUtil.getCrateRoot(match);
            org.rust.ide.utils.checkMatch.UsefulnessResult useful = org.rust.ide.utils.checkMatch.CheckMatchUtils.isUseful(seen, patterns, false, crateRoot, true);
            if (!useful.isUseful()) {
                RsMatchArm arm = RsElementUtil.ancestorStrict(armPat, RsMatchArm.class);
                if (arm == null) return;

                com.intellij.codeInspection.LocalQuickFix fix;
                if (RsMatchArmUtil.getPatList(arm).size() == 1) {
                    // If the arm consists of only one pattern, we can delete the whole arm
                    fix = SubstituteTextFix.delete(
                        RsBundle.message("intention.name.remove.unreachable.match.arm"),
                        match.getContainingFile(),
                        PsiElementExt.getRangeWithPrevSpace(arm)
                    );
                } else {
                    // Otherwise, delete only ` | <pat>` part from the arm
                    PsiElement prevSibling = RsElementUtil.getPrevNonCommentSibling(armPat);
                    TextRange separatorRange = TextRange.EMPTY_RANGE;
                    if (prevSibling instanceof LeafPsiElement
                        && ((LeafPsiElement) prevSibling).getElementType() == RsElementTypes.OR) {
                        separatorRange = PsiElementExt.getRangeWithPrevSpace(prevSibling);
                    }

                    TextRange range = PsiElementExt.getRangeWithPrevSpace(armPat).union(separatorRange);
                    fix = SubstituteTextFix.delete(
                        RsBundle.message("intention.name.remove.unreachable.pattern"),
                        match.getContainingFile(),
                        range
                    );
                }

                holder.registerProblem(armPat, RsBundle.message("inspection.message.unreachable.pattern"), fix);
            }

            // If the arm is not guarded, we have "seen" the pattern
            RsMatchArm armForGuard = RsElementUtil.ancestorStrict(armPat, RsMatchArm.class);
            if (armForGuard != null && armForGuard.getMatchArmGuard() == null) {
                seen.add(patterns);
            }
        }
    }
}
