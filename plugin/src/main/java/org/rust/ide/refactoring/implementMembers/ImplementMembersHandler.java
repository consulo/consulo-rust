/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.implementMembers;

import org.rust.lang.core.psi.ext.RsElementUtil;
import consulo.language.editor.action.LanguageCodeInsightActionHandler;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.RsImplItem;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.openapiext.Testmark;
import org.rust.ide.refactoring.implementMembers.ImplementMembersImpl;

public class ImplementMembersHandler implements LanguageCodeInsightActionHandler {
    @jakarta.annotation.Nonnull @Override public consulo.language.Language getLanguage() { return org.rust.lang.RsLanguage.INSTANCE; }


    @Override
    public boolean isValidFor(@Nonnull Editor editor, @Nonnull PsiFile file) {
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
    public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
        PsiElement elementAtCaret = file.findElementAt(editor.getCaretModel().getOffset());
        RsImplItem implItem = elementAtCaret != null ? RsElementUtil.ancestorOrSelf(elementAtCaret, RsImplItem.class) : null;
        if (implItem == null) {
            throw new IllegalStateException("No impl trait item");
        }
        ImplementMembersImpl.generateTraitMembers(implItem, editor);
    }
}
