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

public class RemoveCastFix extends RsQuickFixBase<RsCastExpr> {

    
    private final String fixText;

    public RemoveCastFix(@Nonnull RsCastExpr element) {
        super(element);
        this.fixText = RsBundle.message("intention.name.remove.as", element.getTypeReference().getText());
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.remove.unnecessary.cast"));
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(fixText);
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsCastExpr element) {
        element.replace(element.getExpr());
    }
}
