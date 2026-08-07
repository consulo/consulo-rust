/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsMethodOrFieldUtil;
import consulo.annotation.component.ExtensionImpl;

@ExtensionImpl
public class RsCStringPointerInspection extends RsLocalInspectionTool {

@Override
    public RsVisitor buildVisitor(@Nonnull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitMethodCall(@Nonnull RsMethodCall asPtrCall) {
                if (!asPtrCall.getReferenceName().equals("as_ptr")) return;

                RsDotExpr parentDot = RsMethodOrFieldUtil.getParentDotExpr(asPtrCall);
                RsExpr parentExpr = parentDot.getExpr();
                if (!(parentExpr instanceof RsDotExpr)) return;
                RsMethodCall unwrapCall = ((RsDotExpr) parentExpr).getMethodCall();
                if (unwrapCall == null || !unwrapCall.getReferenceName().equals("unwrap")) return;

                RsExpr ctorExprRaw = RsMethodOrFieldUtil.getParentDotExpr(unwrapCall).getExpr();
                if (!(ctorExprRaw instanceof RsCallExpr)) return;
                RsCallExpr ctorExpr = (RsCallExpr) ctorExprRaw;
                RsExpr pathExpr = ctorExpr.getExpr();
                if (pathExpr instanceof RsPathExpr) {
                    RsPath path = ((RsPathExpr) pathExpr).getPath();
                    if (path.getIdentifier() != null && path.getIdentifier().getText().equals("new")
                        && path.getPath() != null && path.getPath().getIdentifier() != null
                        && path.getPath().getIdentifier().getText().equals("CString")) {
                        holder.registerProblem(RsMethodOrFieldUtil.getParentDotExpr(asPtrCall), getDisplayName().get());
                    }
                }
            }
        };
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.rs.c.string.pointer.display.name"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("rust"));
    }
}
