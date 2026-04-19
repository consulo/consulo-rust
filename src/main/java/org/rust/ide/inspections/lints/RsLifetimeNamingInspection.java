/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import org.jetbrains.annotations.NotNull;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.ide.inspections.RsWithMacrosInspectionVisitor;
import org.rust.lang.core.psi.RsLifetimeParameter;
import org.rust.lang.core.psi.RsVisitor;

public class RsLifetimeNamingInspection extends RsSnakeCaseNamingInspection {

    public RsLifetimeNamingInspection() {
        super("Lifetime");
    }

    @NotNull
    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitLifetimeParameter(@NotNull RsLifetimeParameter el) {
                inspect(el.getQuoteIdentifier(), holder);
            }
        };
    }
}
