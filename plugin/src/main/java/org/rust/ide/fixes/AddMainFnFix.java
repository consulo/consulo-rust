/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import consulo.language.editor.inspection.LocalQuickFixAndIntentionActionOnPsiElement;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.impl.RsPsiFactory;
import org.rust.lang.core.psi.ext.impl.RsFunctionUtil;
import consulo.localize.LocalizeValue;

public class AddMainFnFix extends LocalQuickFixAndIntentionActionOnPsiElement {

    public AddMainFnFix(@Nonnull PsiElement file) {
        super(file);
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.add.fn.main"));
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return getFamilyName();
    }

    @Override
    public void invoke(@Nonnull Project project, @Nonnull PsiFile file, @Nullable Editor editor,
                       @Nonnull PsiElement startElement, @Nonnull PsiElement endElement) {
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
