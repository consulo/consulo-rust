/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import org.jetbrains.annotations.NotNull;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.ide.inspections.RsWithMacrosInspectionVisitor;
import org.rust.lang.core.psi.RsEnumItem;
import org.rust.lang.core.psi.RsVisitor;

public class RsEnumNamingInspection extends RsCamelCaseNamingInspection {

    public RsEnumNamingInspection() {
        super("Type", "Enum");
    }

    @NotNull
    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitEnumItem2(@NotNull RsEnumItem o) {
                inspect(o.getIdentifier(), holder);
            }
        };
    }
}
