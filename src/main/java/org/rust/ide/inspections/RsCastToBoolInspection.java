/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsCastExpr;
import org.rust.lang.core.psi.RsVisitor;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyBool;
import org.rust.lang.core.types.ty.TyPrimitive;
import org.rust.lang.core.types.ty.TyUnit;
import org.rust.lang.utils.RsDiagnostic;

public class RsCastToBoolInspection extends RsLocalInspectionTool {

    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitCastExpr(@NotNull RsCastExpr castExpr) {
                Ty exprType = RsTypesUtil.getType(castExpr.getExpr());
                if (exprType instanceof TyBool || !(exprType instanceof TyPrimitive) || exprType instanceof TyUnit) return;

                if (RsTypesUtil.getNormType(castExpr.getTypeReference()) instanceof TyBool) {
                    RsDiagnostic.addToHolder(new RsDiagnostic.CastAsBoolError(castExpr), holder);
                }
            }
        };
    }
}
