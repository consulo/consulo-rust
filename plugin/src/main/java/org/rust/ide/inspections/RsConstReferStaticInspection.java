/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import jakarta.annotation.Nonnull;
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
    public RsVisitor buildVisitor(@Nonnull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitPathExpr(@Nonnull RsPathExpr pathExpr) {
                RsConstContextKind constContext = RsExprUtil.getClassifyConstContext(pathExpr);
                if (constContext != null) {
                    checkPathInConstContext(holder, pathExpr.getPath(), constContext);
                }
                super.visitPathExpr(pathExpr);
            }

            @Override
            public void visitPathType(@Nonnull RsPathType o) {
                checkPathInConstContext(holder, o.getPath(), RsConstContextKind.ConstGenericArgument);
                super.visitPathType(o);
            }
        };
    }

    private void checkPathInConstContext(@Nonnull RsProblemsHolder holder, @Nonnull RsPath path, @Nonnull RsConstContextKind constContext) {
        var ref = path.getReference() != null ? path.getReference().resolve() : null;
        if (!(ref instanceof RsConstant)) return;
        if (!RsConstantUtil.isConst((RsConstant) ref)) {
            RsDiagnostic.addToHolder(new RsDiagnostic.ConstItemReferToStaticError(path, constContext), holder);
        }
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.rs.const.refer.static.display.name"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("rust"));
    }
}
