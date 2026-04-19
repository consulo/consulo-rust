/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsConstContextKind;
import org.rust.lang.core.psi.ext.RsExprUtil;
import org.rust.lang.core.psi.ext.RsConstantUtil;
import org.rust.lang.utils.RsDiagnostic;

/**
 * Inspection that detects the E0013 error.
 */
public class RsConstReferStaticInspection extends RsLocalInspectionTool {

    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitPathExpr(@NotNull RsPathExpr pathExpr) {
                RsConstContextKind constContext = RsExprUtil.getClassifyConstContext(pathExpr);
                if (constContext != null) {
                    checkPathInConstContext(holder, pathExpr.getPath(), constContext);
                }
                super.visitPathExpr(pathExpr);
            }

            @Override
            public void visitPathType(@NotNull RsPathType o) {
                checkPathInConstContext(holder, o.getPath(), RsConstContextKind.ConstGenericArgument);
                super.visitPathType(o);
            }
        };
    }

    private void checkPathInConstContext(@NotNull RsProblemsHolder holder, @NotNull RsPath path, @NotNull RsConstContextKind constContext) {
        var ref = path.getReference() != null ? path.getReference().resolve() : null;
        if (!(ref instanceof RsConstant)) return;
        if (!RsConstantUtil.isConst((RsConstant) ref)) {
            RsDiagnostic.addToHolder(new RsDiagnostic.ConstItemReferToStaticError(path, constContext), holder);
        }
    }
}
