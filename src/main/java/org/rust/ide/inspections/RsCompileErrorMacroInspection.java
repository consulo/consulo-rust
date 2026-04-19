/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsMacroDefinitionBase;
import org.rust.lang.core.psi.ext.RsMacroCallUtil;
import org.rust.lang.core.psi.ext.PsiElementUtil;

import java.util.List;
import org.rust.lang.core.psi.ext.RsLitExprUtil;

public class RsCompileErrorMacroInspection extends RsLocalInspectionTool {

    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitMacroCall2(@NotNull RsMacroCall o) {
                RsMacroDefinitionBase resolvedTo = RsMacroCallUtil.resolveToMacro(o);
                if (resolvedTo == null) return;
                if (!resolvedTo.getName().equals("compile_error") || resolvedTo.getContainingCrate().getOrigin() != PackageOrigin.STDLIB) return;
                RsMacroArgument macroArgument = o.getMacroArgument();
                if (macroArgument == null) return;
                List<RsLitExpr> litExprs = macroArgument.getLitExprList();
                if (litExprs.size() != 1) return;
                RsLitExpr messageLiteral = litExprs.get(0);
                RsLiteralKind kind = RsLiteralKindUtil.getKind(messageLiteral);
                if (!(kind instanceof RsLiteralKind.StringLiteral)) return;
                String message = ((RsLiteralKind.StringLiteral) kind).getValue();
                if (message == null) return;
                TextRange errorRange = o.getPath().getTextRange().union(macroArgument.getTextRange()).shiftLeft(PsiElementUtil.getStartOffset(o));
                holder.registerProblem(o, errorRange, message, true);
            }
        };
    }
}
