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
import org.rust.lang.core.psi.RsPsiFactory;

public class EscapeKeywordFix extends RsQuickFixBase<PsiElement> {

    private final boolean isKeyword;

    public EscapeKeywordFix(@NotNull PsiElement element, boolean isKeyword) {
        super(element);
        this.isKeyword = isKeyword;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.name.escape.keyword");
    }

    @NotNull
    @Override
    public String getText() {
        return isKeyword
            ? RsBundle.message("intention.name.escape.keyword")
            : RsBundle.message("intention.name.escape.reserved.keyword");
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull PsiElement element) {
        String name = element.getText();
        element.replace(new RsPsiFactory(project).createIdentifier("r#" + name));
    }
}
