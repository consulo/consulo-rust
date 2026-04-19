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
import org.rust.lang.core.psi.RsExprStmt;

public class RemoveSemicolonFix extends RsQuickFixBase<RsExprStmt> {

    public RemoveSemicolonFix(@NotNull RsExprStmt element) {
        super(element);
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.remove.semicolon");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsExprStmt element) {
        PsiElement semicolon = element.getSemicolon();
        if (semicolon != null) {
            semicolon.delete();
        }
    }
}
