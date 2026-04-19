/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import org.jetbrains.annotations.NotNull;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.ide.inspections.RsWithMacrosInspectionVisitor;
import org.rust.lang.core.psi.RsModDeclItem;
import org.rust.lang.core.psi.RsModItem;
import org.rust.lang.core.psi.RsVisitor;

public class RsModuleNamingInspection extends RsSnakeCaseNamingInspection {

    public RsModuleNamingInspection() {
        super("Module");
    }

    @NotNull
    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitModDeclItem(@NotNull RsModDeclItem el) {
                inspect(el.getIdentifier(), holder);
            }

            @Override
            public void visitModItem2(@NotNull RsModItem o) {
                inspect(o.getIdentifier(), holder);
            }
        };
    }
}
