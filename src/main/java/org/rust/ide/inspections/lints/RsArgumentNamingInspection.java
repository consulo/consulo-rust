/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import org.jetbrains.annotations.NotNull;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.ide.inspections.RsWithMacrosInspectionVisitor;
import org.rust.lang.core.psi.RsPatBinding;
import org.rust.lang.core.psi.RsValueParameter;
import org.rust.lang.core.psi.RsVisitor;

public class RsArgumentNamingInspection extends RsSnakeCaseNamingInspection {

    public RsArgumentNamingInspection() {
        super("Argument");
    }

    @NotNull
    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitPatBinding(@NotNull RsPatBinding el) {
                if (el.getParent() != null && el.getParent().getParent() instanceof RsValueParameter) {
                    inspect(el.getIdentifier(), holder);
                }
            }
        };
    }
}
