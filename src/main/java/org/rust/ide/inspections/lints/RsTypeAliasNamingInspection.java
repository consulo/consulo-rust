/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import org.jetbrains.annotations.NotNull;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.ide.inspections.RsWithMacrosInspectionVisitor;
import org.rust.lang.core.psi.RsTypeAlias;
import org.rust.lang.core.psi.RsVisitor;
import org.rust.lang.core.psi.ext.RsAbstractableOwner;

public class RsTypeAliasNamingInspection extends RsCamelCaseNamingInspection {

    public RsTypeAliasNamingInspection() {
        super("Type", "Type alias");
    }

    @NotNull
    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitTypeAlias2(@NotNull RsTypeAlias o) {
                if (o.getOwner() == RsAbstractableOwner.Free) {
                    inspect(o.getIdentifier(), holder);
                }
            }
        };
    }
}
