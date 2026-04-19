/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.ide.fixes.ChangeToFieldShorthandFix;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsPathExpr;
import org.rust.lang.core.psi.RsStructLiteralField;
import org.rust.lang.core.psi.RsVisitor;

public class RsFieldInitShorthandInspection extends RsLocalInspectionTool {

    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitStructLiteralField(@NotNull RsStructLiteralField o) {
                RsExpr init = o.getExpr();
                if (init == null) return;
                PsiElement identifier = o.getIdentifier();
                if (identifier == null) return;
                if (!(init instanceof RsPathExpr && init.getText().equals(identifier.getText()))) return;
                holder.registerProblem(
                    o,
                    RsBundle.message("inspection.message.expression.can.be.simplified"),
                    ProblemHighlightType.WEAK_WARNING,
                    new ChangeToFieldShorthandFix(o)
                );
            }
        };
    }

    @Override
    public boolean isSyntaxOnly() {
        return true;
    }
}
