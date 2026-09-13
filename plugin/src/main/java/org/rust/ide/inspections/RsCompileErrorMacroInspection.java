/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import consulo.document.util.TextRange;
import jakarta.annotation.Nonnull;
import org.rust.cargo.api.workspace.PackageOrigin;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsMacroDefinitionBase;
import org.rust.lang.core.psi.ext.impl.RsMacroCallUtil;
import org.rust.lang.core.psi.ext.impl.PsiElementUtil;

import java.util.List;
import org.rust.lang.core.psi.ext.impl.RsLitExprUtil;
import consulo.localize.LocalizeValue;
import org.rust.RsBundle;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.annotation.component.ExtensionImpl;
import org.rust.lang.core.psi.impl.*;

@ExtensionImpl
public class RsCompileErrorMacroInspection extends RsLocalInspectionTool {

    @Override
    public RsVisitor buildVisitor(@Nonnull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitMacroCall2(@Nonnull RsMacroCall o) {
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

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.rs.compile.error.macro.display.name"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("rust"));
    }

    @Nonnull
    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return HighlightDisplayLevel.ERROR;
    }
}
