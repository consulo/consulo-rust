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
import org.rust.lang.core.psi.RsParenExpr;
import consulo.localize.LocalizeValue;

public class RemoveRedundantParenthesesFix extends RsQuickFixBase<RsParenExpr> {

    public RemoveRedundantParenthesesFix(@Nonnull RsParenExpr element) {
        super(element);
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.remove.parentheses.from.expression"));
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return getText();
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsParenExpr element) {
        RsExpr wrapped = element.getExpr();
        if (wrapped == null) return;
        element.replace(wrapped);
    }
}
