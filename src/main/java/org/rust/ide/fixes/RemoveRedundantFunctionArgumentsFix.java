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
import org.rust.lang.core.psi.RsValueArgumentList;
import org.rust.lang.core.psi.ext.RsElementUtil;

import java.util.List;
import org.rust.lang.core.psi.ext.PsiElementUtil;

public class RemoveRedundantFunctionArgumentsFix extends RsQuickFixBase<RsValueArgumentList> {

    private final int expectedCount;

    public RemoveRedundantFunctionArgumentsFix(@NotNull RsValueArgumentList element, int expectedCount) {
        super(element);
        this.expectedCount = expectedCount;
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.remove.redundant.arguments");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsValueArgumentList element) {
        List<RsExpr> exprList = element.getExprList();
        List<RsExpr> extraArgs = exprList.subList(expectedCount, exprList.size());
        for (RsExpr arg : extraArgs) {
            PsiElementUtil.deleteWithSurroundingCommaAndWhitespace(arg);
        }
    }
}
