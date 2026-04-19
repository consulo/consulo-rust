/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.implementMembers;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.lang.LanguageCodeInsightActionHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.RsImplItem;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.openapiext.Testmark;
import org.rust.ide.refactoring.implementMembers.ImplementMembersImpl;

public class ImplementMembersHandler implements LanguageCodeInsightActionHandler {

    @Override
    public boolean isValidFor(@NotNull Editor editor, @NotNull PsiFile file) {
        if (!(file instanceof RsFile)) return false;

        PsiElement elementAtCaret = file.findElementAt(editor.getCaretModel().getOffset());
        RsImplItem implItem = elementAtCaret != null ? RsElementUtil.ancestorOrSelf(elementAtCaret, RsImplItem.class) : null;
        if (implItem == null) {
            ImplementMembersMarks.NoImplInHandler.hit();
            return false;
        }
        return true;
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        PsiElement elementAtCaret = file.findElementAt(editor.getCaretModel().getOffset());
        RsImplItem implItem = elementAtCaret != null ? RsElementUtil.ancestorOrSelf(elementAtCaret, RsImplItem.class) : null;
        if (implItem == null) {
            throw new IllegalStateException("No impl trait item");
        }
        ImplementMembersImpl.generateTraitMembers(implItem, editor);
    }
}
