/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import org.jetbrains.annotations.NotNull;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.ide.inspections.RsWithMacrosInspectionVisitor;
import org.rust.lang.core.psi.RsEnumVariant;
import org.rust.lang.core.psi.RsVisitor;

public class RsEnumVariantNamingInspection extends RsCamelCaseNamingInspection {

    public RsEnumVariantNamingInspection() {
        super("Enum variant");
    }

    @NotNull
    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitEnumVariant(@NotNull RsEnumVariant el) {
                inspect(el.getIdentifier(), holder);
            }
        };
    }
}
