/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import jakarta.annotation.Nonnull;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.ide.inspections.RsWithMacrosInspectionVisitor;
import org.rust.lang.core.psi.RsTypeAlias;
import org.rust.lang.core.psi.RsVisitor;
import org.rust.lang.core.psi.ext.RsAbstractableOwner;
import consulo.localize.LocalizeValue;
import org.rust.RsBundle;
import consulo.annotation.component.ExtensionImpl;

@ExtensionImpl
public class RsAssocTypeNamingInspection extends RsCamelCaseNamingInspection {

    public RsAssocTypeNamingInspection() {
        super("Type", "Associated type");
    }

    @Nonnull
    @Override
    public RsVisitor buildVisitor(@Nonnull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitTypeAlias2(@Nonnull RsTypeAlias o) {
                if (o.getOwner() instanceof RsAbstractableOwner.Trait) {
                    inspect(o.getIdentifier(), holder);
                }
            }
        };
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.rs.assoc.type.naming.display.name"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("naming.conventions"));
    }

    @Nonnull
    @Override
    public LocalizeValue[] getGroupPath() {
        return new LocalizeValue[]{LocalizeValue.of(RsBundle.message("rust")), LocalizeValue.of(RsBundle.message("lints"))};
    }
}
