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
import org.rust.lang.core.psi.ext.RsElement;

public class EncloseExprInBracesFix extends RsQuickFixBase<PsiElement> {

    public EncloseExprInBracesFix(@NotNull RsElement element) {
        super(element);
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.enclose.expression.in.braces");
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull PsiElement element) {
        var enclosed = new RsPsiFactory(project).createExpression("{ " + element.getText() + " }");
        element.replace(enclosed);
    }
}
