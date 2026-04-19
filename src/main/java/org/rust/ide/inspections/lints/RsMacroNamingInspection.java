/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import org.jetbrains.annotations.NotNull;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.ide.inspections.RsWithMacrosInspectionVisitor;
import org.rust.lang.core.psi.RsMacro;
import org.rust.lang.core.psi.RsVisitor;

public class RsMacroNamingInspection extends RsSnakeCaseNamingInspection {

    public RsMacroNamingInspection() {
        super("Macro");
    }

    @NotNull
    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitMacro2(@NotNull RsMacro o) {
                inspect(o.getNameIdentifier(), holder);
            }
        };
    }
}
