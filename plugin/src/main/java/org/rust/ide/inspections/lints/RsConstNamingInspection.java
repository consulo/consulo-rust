/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import jakarta.annotation.Nonnull;
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

    @Nonnull
    @Override
    public RsVisitor buildVisitor(@Nonnull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitConstant2(@Nonnull RsConstant o) {
                if (RsConstantUtil.getKind(o) == RsConstantKind.CONST) {
                    inspect(o.getIdentifier(), holder);
                }
            }
        };
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.rs.const.naming.display.name"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("naming.conventions"));
    }
}
