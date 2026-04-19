/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import org.rust.lang.core.psi.ext.RsElementUtil;
import org.jetbrains.annotations.NotNull;
import org.rust.ide.fixes.AddMutableFix;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsBinaryExprUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.types.DeclarationUtil;
import org.rust.lang.core.types.MutabilityUtil;
import org.rust.lang.utils.RsDiagnostic;

import com.intellij.psi.PsiElement;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ExtensionsUtil;

public class RsReassignImmutableInspection extends RsLocalInspectionTool {

    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitBinaryExpr(@NotNull RsBinaryExpr expr) {
                RsExpr left = expr.getLeft();
                if (!ExtensionsUtil.isImmutable(left)) return;

                if (RsBinaryExprUtil.isAssignBinaryExpr(expr) && left instanceof RsPathExpr) {
                    // TODO: perform some kind of data-flow analysis
                    PsiElement declaration = RsTypesUtil.getDeclaration(left);
                    RsLetDecl letExpr = declaration != null ? RsElementUtil.ancestorStrict(declaration, RsLetDecl.class) : null;
                    if (letExpr == null) {
                        registerProblem(holder, expr, left);
                    } else if (letExpr.getEq() != null) {
                        registerProblem(holder, expr, left);
                    }
                }
            }
        };
    }

    private static void registerProblem(@NotNull RsProblemsHolder holder, @NotNull RsExpr expr, @NotNull RsExpr nameExpr) {
        AddMutableFix fix = AddMutableFix.createIfCompatible(nameExpr);
        new RsDiagnostic.CannotReassignToImmutable(expr, fix).addToHolder(holder);
    }
}
