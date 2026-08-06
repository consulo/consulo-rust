/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.ide.fixes.RemoveSemicolonFix;
import org.rust.lang.core.dfa.ExitPoint;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsTypeReference;
import org.rust.lang.core.psi.RsVisitor;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.TyUnit;

/**
 * Suggest to remove a semicolon in situations like
 *
 * <pre>
 * fn foo() -> i32 { 92; }
 * </pre>
 */
public class RsExtraSemicolonInspection extends RsLocalInspectionTool {

@Override
    public RsVisitor buildVisitor(@Nonnull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitFunction2(@Nonnull RsFunction o) {
                if (o.getRetType() == null) return;
                RsTypeReference retType = o.getRetType().getTypeReference();
                if (retType == null) return;
                if (RsTypesUtil.getNormType(retType) instanceof TyUnit) return;
                ExitPoint.process(o, exitPoint -> {
                    if (exitPoint instanceof ExitPoint.InvalidTailStatement) {
                        ExitPoint.InvalidTailStatement invalidTail = (ExitPoint.InvalidTailStatement) exitPoint;
                        holder.registerProblem(
                            invalidTail.stmt,
                            RsBundle.message("inspection.message.function.returns.instead", retType.getText()),
                            new RemoveSemicolonFix(invalidTail.stmt)
                        );
                    }
                });
            }
        };
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.rs.extra.semicolon.display.name"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("rust"));
    }
}
