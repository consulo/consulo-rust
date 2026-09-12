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
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.RsUnaryExpr;
import consulo.localize.LocalizeValue;

public class ReplaceBoxSyntaxFix extends RsQuickFixBase<RsUnaryExpr> {

    public ReplaceBoxSyntaxFix(@Nonnull RsUnaryExpr element) {
        super(element);
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.replace.box.with.box.new"));
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return getFamilyName();
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsUnaryExpr element) {
        if (element.getBox() == null) return;
        RsExpr expr = element.getExpr();
        if (expr == null) return;
        element.replace(new RsPsiFactory(project).createBox(expr.getText()));
    }
}
