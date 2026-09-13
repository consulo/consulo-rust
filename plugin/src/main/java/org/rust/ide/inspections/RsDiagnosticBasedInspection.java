/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsInferenceContextOwner;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.utils.RsInferenceDiagnostic;

public abstract class RsDiagnosticBasedInspection extends RsLocalInspectionTool {

    @Override
    public RsVisitor buildVisitor(@Nonnull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitFunction2(@Nonnull RsFunction o) { collectDiagnostics(holder, o); }
            @Override
            public void visitConstant2(@Nonnull RsConstant o) { collectDiagnostics(holder, o); }
            @Override
            public void visitConstParameter(@Nonnull RsConstParameter o) { collectDiagnostics(holder, o); }
            @Override
            public void visitArrayType(@Nonnull RsArrayType o) { collectDiagnostics(holder, o); }
            @Override
            public void visitPath(@Nonnull RsPath o) { collectDiagnostics(holder, o); }
            @Override
            public void visitVariantDiscriminant(@Nonnull RsVariantDiscriminant o) { collectDiagnostics(holder, o); }
            @Override
            public void visitDefaultParameterValue(@Nonnull RsDefaultParameterValue o) { collectDiagnostics(holder, o); }
        };
    }

    private void collectDiagnostics(@Nonnull RsProblemsHolder holder, @Nonnull RsInferenceContextOwner element) {
        for (RsInferenceDiagnostic reported : RsTypesUtil.getSelfInferenceResult(element).getDiagnostics()) {
            RsDiagnostic diagnostic = RsDiagnostic.of(reported);
            if (diagnostic != null && diagnostic.getInspectionClass() == getClass()) {
                RsDiagnostic.addToHolder(diagnostic, holder);
            }
        }
    }
}
