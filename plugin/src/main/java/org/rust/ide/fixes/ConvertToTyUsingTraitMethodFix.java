/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.impl.RsPsiFactory;
import org.rust.lang.core.types.ty.Ty;

/**
 * For the given {@code expr} adds method call defined by methodName.
 */
public abstract class ConvertToTyUsingTraitMethodFix extends ConvertToTyUsingTraitFix {
    private final String myMethodName;

    public ConvertToTyUsingTraitMethodFix(@Nonnull RsExpr expr, @Nonnull String tyName, @Nonnull String traitName, @Nonnull String methodName) {
        super(expr, tyName, traitName);
        this.myMethodName = methodName;
    }

    public ConvertToTyUsingTraitMethodFix(@Nonnull RsExpr expr, @Nonnull Ty ty, @Nonnull String traitName, @Nonnull String methodName) {
        super(expr, ty, traitName);
        this.myMethodName = methodName;
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsExpr element) {
        element.replace(new RsPsiFactory(project).createNoArgsMethodCall(element, myMethodName));
    }
}
