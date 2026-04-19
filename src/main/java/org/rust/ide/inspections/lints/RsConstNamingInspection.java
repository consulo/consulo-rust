/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import org.jetbrains.annotations.NotNull;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.ide.inspections.RsWithMacrosInspectionVisitor;
import org.rust.lang.core.psi.RsConstant;
import org.rust.lang.core.psi.ext.RsConstantKind;
import org.rust.lang.core.psi.RsVisitor;
import org.rust.lang.core.psi.ext.RsConstantUtil;

public class RsConstNamingInspection extends RsUpperCaseNamingInspection {

    public RsConstNamingInspection() {
        super("Constant");
    }

    @NotNull
    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitConstant2(@NotNull RsConstant o) {
                if (RsConstantUtil.getKind(o) == RsConstantKind.CONST) {
                    inspect(o.getIdentifier(), holder);
                }
            }
        };
    }
}
