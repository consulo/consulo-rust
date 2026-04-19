/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.types.ty.Ty;

/**
 * For the given {@code expr} adds method call defined by methodName.
 */
public abstract class ConvertToTyUsingTraitMethodFix extends ConvertToTyUsingTraitFix {
    private final String myMethodName;

    public ConvertToTyUsingTraitMethodFix(@NotNull RsExpr expr, @NotNull String tyName, @NotNull String traitName, @NotNull String methodName) {
        super(expr, tyName, traitName);
        this.myMethodName = methodName;
    }

    public ConvertToTyUsingTraitMethodFix(@NotNull RsExpr expr, @NotNull Ty ty, @NotNull String traitName, @NotNull String methodName) {
        super(expr, ty, traitName);
        this.myMethodName = methodName;
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsExpr element) {
        element.replace(new RsPsiFactory(project).createNoArgsMethodCall(element, myMethodName));
    }
}
