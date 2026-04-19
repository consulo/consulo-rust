/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsStructLiteralField;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.lang.core.psi.ext.PsiElementUtil;

public class RemoveStructLiteralFieldFix extends RsQuickFixBase<PsiElement> {

    private final String removingFieldName;

    public RemoveStructLiteralFieldFix(@NotNull RsStructLiteralField field) {
        this(field, "`" + field.getText() + "`");
    }

    public RemoveStructLiteralFieldFix(@NotNull RsStructLiteralField field, @NotNull String removingFieldName) {
        super(field);
        this.removingFieldName = removingFieldName;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.remove.struct.literal.field");
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.remove2", removingFieldName);
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull PsiElement startElement) {
        if (!(startElement instanceof RsStructLiteralField)) return;
        RsStructLiteralField field = (RsStructLiteralField) startElement;
        PsiElementUtil.deleteWithSurroundingCommaAndWhitespace(field);
    }
}
