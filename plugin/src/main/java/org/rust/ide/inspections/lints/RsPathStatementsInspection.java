/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.ide.fixes.RemoveElementFix;
import org.rust.ide.fixes.SubstituteTextFix;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.ide.inspections.RsWithMacrosInspectionVisitor;
import org.rust.lang.core.psi.RsExprStmt;
import org.rust.lang.core.psi.RsPathExpr;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsVisitor;
// import org.rust.lang.core.types.ImplLookupExtensionsUtil; // placeholder
import org.rust.lang.core.types.infer.NeedsDropUtil;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.utils.evaluation.ThreeValuedLogic;

import java.util.Collections;
import java.util.List;
import org.rust.lang.core.psi.ext.RsStmtUtil;
import org.rust.lang.core.resolve.ImplLookup;
import consulo.localize.LocalizeValue;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.annotation.component.ExtensionImpl;

// TODO: Future improvements: https://github.com/intellij-rust/intellij-rust/issues/9555
//  The inspection is currently disabled by default.
/** Analogue of https://doc.rust-lang.org/rustc/lints/listing/warn-by-default.html#path-statements */
@ExtensionImpl
public class RsPathStatementsInspection extends RsLintInspection {

    @Nonnull
    @Override
    protected RsLint getLint(@Nonnull PsiElement element) {
        return RsLint.PathStatements;
    }

    @Nonnull
    @Override
    public RsVisitor buildVisitor(@Nonnull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitExprStmt(@Nonnull RsExprStmt exprStmt) {
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

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.rs.path.statements.display.name"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("lints"));
    }

    @Nonnull
    @Override
    public LocalizeValue[] getGroupPath() {
        return new LocalizeValue[]{LocalizeValue.of(RsBundle.message("rust"))};
    }

    @Nonnull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.WEAK_WARNING;
    }
}
