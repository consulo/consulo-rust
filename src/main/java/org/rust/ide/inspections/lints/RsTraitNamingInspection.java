/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import org.jetbrains.annotations.NotNull;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.ide.inspections.RsWithMacrosInspectionVisitor;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.RsVisitor;

public class RsTraitNamingInspection extends RsCamelCaseNamingInspection {

    public RsTraitNamingInspection() {
        super("Trait");
    }

    @NotNull
    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitTraitItem2(@NotNull RsTraitItem o) {
                inspect(o.getIdentifier(), holder);
            }
        };
    }
}
