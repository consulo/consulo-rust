/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.ide.inspections.RsWithMacrosInspectionVisitor;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsVisitor;
import org.rust.lang.core.psi.ext.RsAbstractableOwner;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.lang.core.psi.ext.RsOuterAttributeOwnerUtil;
import org.rust.lang.core.psi.ext.RsAttrOwnerExtUtil;
import org.rust.lang.core.psi.ext.RsDocAndAttributeOwnerUtil;

public class RsFunctionNamingInspection extends RsSnakeCaseNamingInspection {

    public RsFunctionNamingInspection() {
        super("Function");
    }

    @Nonnull
    @Override
    public RsVisitor buildVisitor(@Nonnull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitFunction2(@Nonnull RsFunction o) {
                if (o.getOwner() == RsAbstractableOwner.Free) {
                    inspect(o.getIdentifier(), holder);
                }
            }
        };
    }

    @Override
    public boolean isSuppressedFor(@Nonnull PsiElement element) {
        if (super.isSuppressedFor(element)) return true;
        if (!(element.getParent() instanceof RsFunction)) return false;
        RsFunction function = (RsFunction) element.getParent();
        return RsFunctionUtil.isActuallyExtern(function)
            && RsDocAndAttributeOwnerUtil.findOuterAttr(function, "no_mangle") != null;
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.rs.function.naming.display.name"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("naming.conventions"));
    }
}
