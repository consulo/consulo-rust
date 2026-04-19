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

public class RemoveElementFix extends RsQuickFixBase<PsiElement> {

    private final String removingElementName;

    public RemoveElementFix(@NotNull PsiElement element) {
        this(element, "`" + element.getText() + "`");
    }

    public RemoveElementFix(@NotNull PsiElement element, @NotNull String removingElementName) {
        super(element);
        this.removingElementName = removingElementName;
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.remove");
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.remove", removingElementName);
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull PsiElement element) {
        element.delete();
    }
}
