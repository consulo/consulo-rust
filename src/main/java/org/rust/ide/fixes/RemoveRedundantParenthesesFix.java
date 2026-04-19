/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsParenExpr;

public class RemoveRedundantParenthesesFix extends RsQuickFixBase<RsParenExpr> {

    public RemoveRedundantParenthesesFix(@NotNull RsParenExpr element) {
        super(element);
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.remove.parentheses.from.expression");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsParenExpr element) {
        RsExpr wrapped = element.getExpr();
        if (wrapped == null) return;
        element.replace(wrapped);
    }
}
