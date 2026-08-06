/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.ide.fixes.ChangeTryMacroToTryOperator;
import org.rust.lang.core.macros.MacroExpansionContextUtil;
import org.rust.lang.core.psi.RsMacroCall;
import org.rust.lang.core.psi.RsVisitor;
import org.rust.lang.core.psi.ext.RsMacroCallUtil;

/**
 * Change {@code try!} macro to {@code ?} operator.
 */
public class RsTryMacroInspection extends RsLocalInspectionTool {

    @SuppressWarnings("DialogTitleCapitalization")
    @Nonnull
@Override
    public RsVisitor buildVisitor(@Nonnull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitMacroCall2(@Nonnull RsMacroCall o) {
                boolean isApplicable = MacroExpansionContextUtil.isExprOrStmtContext(o) && RsMacroCallUtil.isStdTryMacro(o);
                if (!isApplicable) return;
                holder.registerProblem(
                    o,
                    RsBundle.message("inspection.message.try.macro.can.be.replaced.with.operator"),
                    new ChangeTryMacroToTryOperator(o)
                );
            }
        };
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.rs.try.macro.display.name"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("rust"));
    }
}
