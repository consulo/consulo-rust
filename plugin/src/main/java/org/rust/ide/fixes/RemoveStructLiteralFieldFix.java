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
import org.rust.lang.core.psi.ext.impl.RsElementUtil;
import org.rust.lang.core.psi.ext.impl.PsiElementUtil;
import consulo.localize.LocalizeValue;

public class RemoveStructLiteralFieldFix extends RsQuickFixBase<PsiElement> {

    private final String removingFieldName;

    public RemoveStructLiteralFieldFix(@Nonnull RsStructLiteralField field) {
        this(field, "`" + field.getText() + "`");
    }

    public RemoveStructLiteralFieldFix(@Nonnull RsStructLiteralField field, @Nonnull String removingFieldName) {
        super(field);
        this.removingFieldName = removingFieldName;
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.remove.struct.literal.field"));
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.remove2", removingFieldName));
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull PsiElement startElement) {
        if (!(startElement instanceof RsStructLiteralField)) return;
        RsStructLiteralField field = (RsStructLiteralField) startElement;
        PsiElementUtil.deleteWithSurroundingCommaAndWhitespace(field);
    }
}
