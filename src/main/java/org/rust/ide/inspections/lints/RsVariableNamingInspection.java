/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.ide.inspections.RsWithMacrosInspectionVisitor;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsPatBindingUtil;

public class RsVariableNamingInspection extends RsSnakeCaseNamingInspection {

    public RsVariableNamingInspection() {
        super("Variable");
    }

    @NotNull
    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitPatBinding(@NotNull RsPatBinding el) {
                if (RsPatBindingUtil.isReferenceToConstant(el)) return;

                RsPat pattern = PsiTreeUtil.getTopmostParentOfType(el, RsPat.class);
                if (pattern == null) return;
                if (pattern.getParent() instanceof RsLetDecl) {
                    inspect(el.getIdentifier(), holder);
                }
            }
        };
    }
}
