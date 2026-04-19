/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.ide.fixes.RemoveElementFix;
import org.rust.ide.fixes.SubstituteTextFix;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.ide.inspections.RsWithMacrosInspectionVisitor;
import org.rust.lang.core.psi.RsExprStmt;
import org.rust.lang.core.psi.RsPathExpr;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsVisitor;
import org.rust.lang.core.psi.ext.RsExprStmtExtUtil;
// import org.rust.lang.core.types.ImplLookupExtensionsUtil; // placeholder
import org.rust.lang.core.types.infer.NeedsDropUtil;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.utils.evaluation.ThreeValuedLogic;

import java.util.Collections;
import java.util.List;
import org.rust.lang.core.psi.ext.RsStmtUtil;
import org.rust.lang.core.resolve.ImplLookup;

// TODO: Future improvements: https://github.com/intellij-rust/intellij-rust/issues/9555
//  The inspection is currently disabled by default.
/** Analogue of https://doc.rust-lang.org/rustc/lints/listing/warn-by-default.html#path-statements */
public class RsPathStatementsInspection extends RsLintInspection {

    @NotNull
    @Override
    protected RsLint getLint(@NotNull PsiElement element) {
        return RsLint.PathStatements;
    }

    @NotNull
    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitExprStmt(@NotNull RsExprStmt exprStmt) {
                super.visitExprStmt(exprStmt);

                RsExpr expr = exprStmt.getExpr();
                if (expr instanceof RsPathExpr && !RsStmtUtil.isTailStmt(exprStmt)) {
                    RsLintHighlightingType highlighting = RsLintHighlightingType.WEAK_WARNING;
                    ThreeValuedLogic needsDrop = NeedsDropUtil.needsDrop(
                        RsTypesUtil.getImplLookup(expr),
                        RsTypesUtil.getType(expr),
                        expr
                    );

                    String description;
                    List<LocalQuickFix> fixes;
                    if (needsDrop == ThreeValuedLogic.True) {
                        description = RsBundle.message("inspection.PathStatementsInspection.description.drops.value");
                        fixes = Collections.singletonList(
                            SubstituteTextFix.replace(
                                RsBundle.message("intention.name.use.drop.to.clarify.intent.drop", expr.getText()),
                                expr.getContainingFile(),
                                expr.getTextRange(),
                                "drop(" + expr.getText() + ")"
                            )
                        );
                    } else if (needsDrop == ThreeValuedLogic.False) {
                        description = RsBundle.message("inspection.PathStatementsInspection.description.no.effect");
                        fixes = Collections.singletonList(new RemoveElementFix(exprStmt));
                    } else {
                        return;
                    }
                    registerLintProblem(holder, exprStmt, description, highlighting, fixes);
                }
            }
        };
    }
}
