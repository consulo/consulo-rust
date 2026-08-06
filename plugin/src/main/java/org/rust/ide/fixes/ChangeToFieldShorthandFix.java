/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsStructLiteralField;

public class ChangeToFieldShorthandFix extends RsQuickFixBase<RsStructLiteralField> {

    public ChangeToFieldShorthandFix(@Nonnull RsStructLiteralField element) {
        super(element);
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.use.initialization.shorthand"));
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return getText();
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsStructLiteralField element) {
        applyShorthandInit(element);
    }

    public static void applyShorthandInit(@Nonnull RsStructLiteralField field) {
        PsiElement expr = field.getExpr();
        if (expr != null) expr.delete();
        PsiElement colon = field.getColon();
        if (colon != null) colon.delete();
    }
}
