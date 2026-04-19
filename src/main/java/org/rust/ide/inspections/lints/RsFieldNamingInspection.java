/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import org.jetbrains.annotations.NotNull;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.ide.inspections.RsWithMacrosInspectionVisitor;
import org.rust.lang.core.psi.RsNamedFieldDecl;
import org.rust.lang.core.psi.RsVisitor;

public class RsFieldNamingInspection extends RsSnakeCaseNamingInspection {

    public RsFieldNamingInspection() {
        super("Field");
    }

    @NotNull
    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitNamedFieldDecl(@NotNull RsNamedFieldDecl o) {
                inspect(o.getIdentifier(), holder);
            }
        };
    }
}
