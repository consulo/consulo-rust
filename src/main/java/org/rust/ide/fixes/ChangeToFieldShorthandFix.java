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

public class ChangeToFieldShorthandFix extends RsQuickFixBase<RsStructLiteralField> {

    public ChangeToFieldShorthandFix(@NotNull RsStructLiteralField element) {
        super(element);
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.use.initialization.shorthand");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsStructLiteralField element) {
        applyShorthandInit(element);
    }

    public static void applyShorthandInit(@NotNull RsStructLiteralField field) {
        PsiElement expr = field.getExpr();
        if (expr != null) expr.delete();
        PsiElement colon = field.getColon();
        if (colon != null) colon.delete();
    }
}
