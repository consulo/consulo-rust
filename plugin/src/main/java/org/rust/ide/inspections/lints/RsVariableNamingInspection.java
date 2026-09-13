/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import consulo.language.psi.util.PsiTreeUtil;
import jakarta.annotation.Nonnull;
import org.rust.ide.inspections.RsProblemsHolder;
import org.rust.ide.inspections.RsWithMacrosInspectionVisitor;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.impl.RsPatBindingUtil;
import consulo.localize.LocalizeValue;
import org.rust.RsBundle;
import consulo.annotation.component.ExtensionImpl;

@ExtensionImpl
public class RsVariableNamingInspection extends RsSnakeCaseNamingInspection {

    public RsVariableNamingInspection() {
        super("Variable");
    }

    @Nonnull
    @Override
    public RsVisitor buildVisitor(@Nonnull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @Override
            public void visitPatBinding(@Nonnull RsPatBinding el) {
                if (RsPatBindingUtil.isReferenceToConstant(el)) return;

                RsPat pattern = PsiTreeUtil.getTopmostParentOfType(el, RsPat.class);
                if (pattern == null) return;
                if (pattern.getParent() instanceof RsLetDecl) {
                    inspect(el.getIdentifier(), holder);
                }
            }
        };
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.rs.variable.naming.display.name"));
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
