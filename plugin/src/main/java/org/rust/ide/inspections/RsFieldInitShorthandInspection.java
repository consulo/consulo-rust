/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.ide.fixes.ChangeToFieldShorthandFix;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsPathExpr;
import org.rust.lang.core.psi.RsStructLiteralField;
import org.rust.lang.core.psi.RsVisitor;
import consulo.localize.LocalizeValue;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.annotation.component.ExtensionImpl;

@ExtensionImpl
public class RsFieldInitShorthandInspection extends RsLocalInspectionTool {

    @Override
    public RsVisitor buildVisitor(@Nonnull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitStructLiteralField(@Nonnull RsStructLiteralField o) {
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

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.rs.field.init.shorthand.display.name"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("rust"));
    }

    @Nonnull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.WEAK_WARNING;
    }
}
