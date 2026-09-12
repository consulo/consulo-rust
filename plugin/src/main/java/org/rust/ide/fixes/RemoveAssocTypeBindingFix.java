/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsAssocTypeBinding;
import org.rust.lang.core.psi.RsTypeArgumentList;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.lang.core.psi.ext.PsiElementUtil;
import consulo.localize.LocalizeValue;

public class RemoveAssocTypeBindingFix extends RsQuickFixBase<PsiElement> {

    public RemoveAssocTypeBindingFix(@Nonnull PsiElement binding) {
        super(binding);
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return getText();
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.remove.redundant.associated.type"));
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull PsiElement element) {
        if (!(element instanceof RsAssocTypeBinding)) return;
        RsAssocTypeBinding binding = (RsAssocTypeBinding) element;
        RsTypeArgumentList parent = binding.getParent() instanceof RsTypeArgumentList
            ? (RsTypeArgumentList) binding.getParent()
            : null;

        PsiElementUtil.deleteWithSurroundingCommaAndWhitespace(binding);

        if (parent != null && PsiTreeUtil.getChildOfType(parent, RsElement.class) == null) {
            parent.delete();
        }
    }
}
