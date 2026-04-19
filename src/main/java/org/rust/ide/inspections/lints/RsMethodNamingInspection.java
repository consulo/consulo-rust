/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import org.jetbrains.annotations.NotNull;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.ide.inspections.RsWithMacrosInspectionVisitor;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsVisitor;
import org.rust.lang.core.psi.ext.RsAbstractableOwner;

public class RsMethodNamingInspection extends RsSnakeCaseNamingInspection {

    public RsMethodNamingInspection() {
        super("Method");
    }

    @NotNull
    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitFunction2(@NotNull RsFunction o) {
                RsAbstractableOwner owner = o.getOwner();
                if (owner instanceof RsAbstractableOwner.Trait || owner instanceof RsAbstractableOwner.Impl) {
                    inspect(o.getIdentifier(), holder);
                }
            }
        };
    }
}
