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
import org.rust.lang.core.psi.RsPsiFactory;

/**
 * For the given {@code expr} adds {@code as_str()}/{@code as_mut_str()} method call.
 */
public abstract class ConvertToStrFix extends ConvertToTyFix {
    private final String myStrMethodName;

    public ConvertToStrFix(@Nonnull RsExpr expr, @Nonnull String strTypeName, @Nonnull String strMethodName) {
        super(expr, strTypeName, "`" + strMethodName + "` method");
        this.myStrMethodName = strMethodName;
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsExpr element) {
        element.replace(new RsPsiFactory(project).createNoArgsMethodCall(element, myStrMethodName));
    }
}
