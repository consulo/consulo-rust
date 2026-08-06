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
import org.rust.lang.core.psi.RsCastExpr;
import org.rust.lang.core.psi.RsPsiFactory;

public class ReplaceCastWithLiteralSuffixFix extends RsQuickFixBase<RsCastExpr> {

    
    private final String fixText;

    public ReplaceCastWithLiteralSuffixFix(@Nonnull RsCastExpr element) {
        super(element);
        this.fixText = RsBundle.message("intention.name.replace.with.0.1",
            element.getExpr().getText(), element.getTypeReference().getText());
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.replace.cast.with.literal.suffix"));
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(fixText);
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsCastExpr element) {
        RsPsiFactory psiFactory = new RsPsiFactory(project);
        element.replace(psiFactory.createExpression(element.getExpr().getText() + element.getTypeReference().getText()));
    }
}
