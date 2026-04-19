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

/**
 * For the given {@code expr} adds {@code as_str()}/{@code as_mut_str()} method call.
 */
public abstract class ConvertToStrFix extends ConvertToTyFix {
    private final String myStrMethodName;

    public ConvertToStrFix(@NotNull RsExpr expr, @NotNull String strTypeName, @NotNull String strMethodName) {
        super(expr, strTypeName, "`" + strMethodName + "` method");
        this.myStrMethodName = strMethodName;
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsExpr element) {
        element.replace(new RsPsiFactory(project).createNoArgsMethodCall(element, myStrMethodName));
    }
}
