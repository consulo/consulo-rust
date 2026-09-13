/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsValueArgumentList;
import org.rust.lang.core.psi.ext.impl.RsElementUtil;

import java.util.List;
import org.rust.lang.core.psi.ext.impl.PsiElementUtil;
import consulo.localize.LocalizeValue;

public class RemoveRedundantFunctionArgumentsFix extends RsQuickFixBase<RsValueArgumentList> {

    private final int expectedCount;

    public RemoveRedundantFunctionArgumentsFix(@Nonnull RsValueArgumentList element, int expectedCount) {
        super(element);
        this.expectedCount = expectedCount;
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.remove.redundant.arguments"));
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return getText();
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsValueArgumentList element) {
        List<RsExpr> exprList = element.getExprList();
        List<RsExpr> extraArgs = exprList.subList(expectedCount, exprList.size());
        for (RsExpr arg : extraArgs) {
            PsiElementUtil.deleteWithSurroundingCommaAndWhitespace(arg);
        }
    }
}
