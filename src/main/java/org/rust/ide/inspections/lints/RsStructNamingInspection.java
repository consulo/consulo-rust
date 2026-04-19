/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import org.jetbrains.annotations.NotNull;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.ide.inspections.RsWithMacrosInspectionVisitor;
import org.rust.lang.core.psi.RsStructItem;
import org.rust.lang.core.psi.RsVisitor;

public class RsStructNamingInspection extends RsCamelCaseNamingInspection {

    public RsStructNamingInspection() {
        super("Type", "Struct");
    }

    @NotNull
    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitStructItem2(@NotNull RsStructItem o) {
                inspect(o.getIdentifier(), holder);
            }
        };
    }
}
