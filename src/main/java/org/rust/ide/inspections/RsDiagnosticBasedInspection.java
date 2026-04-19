/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsInferenceContextOwner;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.utils.RsDiagnostic;

public abstract class RsDiagnosticBasedInspection extends RsLocalInspectionTool {

    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitFunction2(@NotNull RsFunction o) { collectDiagnostics(holder, o); }
            @Override
            public void visitConstant2(@NotNull RsConstant o) { collectDiagnostics(holder, o); }
            @Override
            public void visitConstParameter(@NotNull RsConstParameter o) { collectDiagnostics(holder, o); }
            @Override
            public void visitArrayType(@NotNull RsArrayType o) { collectDiagnostics(holder, o); }
            @Override
            public void visitPath(@NotNull RsPath o) { collectDiagnostics(holder, o); }
            @Override
            public void visitVariantDiscriminant(@NotNull RsVariantDiscriminant o) { collectDiagnostics(holder, o); }
            @Override
            public void visitDefaultParameterValue(@NotNull RsDefaultParameterValue o) { collectDiagnostics(holder, o); }
        };
    }

    private void collectDiagnostics(@NotNull RsProblemsHolder holder, @NotNull RsInferenceContextOwner element) {
        for (RsDiagnostic diagnostic : RsTypesUtil.getSelfInferenceResult(element).getDiagnostics()) {
            if (diagnostic.getInspectionClass() == getClass()) {
                RsDiagnostic.addToHolder(diagnostic, holder);
            }
        }
    }
}
