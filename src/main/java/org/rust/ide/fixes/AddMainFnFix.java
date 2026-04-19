/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.ext.RsFunctionUtil;

public class AddMainFnFix extends LocalQuickFixAndIntentionActionOnPsiElement {

    public AddMainFnFix(@NotNull PsiElement file) {
        super(file);
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.add.fn.main");
    }

    @NotNull
    @Override
    public String getText() {
        return getFamilyName();
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull PsiFile file, @Nullable Editor editor,
                       @NotNull PsiElement startElement, @NotNull PsiElement endElement) {
        RsFunction function = (RsFunction) file.add(new RsPsiFactory(project).createFunction("fn main() { }"));
        var block = RsFunctionUtil.getBlock(function);
        if (block == null) return;
        var lbrace = block.getLbrace();
        if (lbrace == null) return;
        if (editor != null) {
            editor.getCaretModel().moveToOffset(lbrace.getTextOffset() + 1);
        }
    }
}
