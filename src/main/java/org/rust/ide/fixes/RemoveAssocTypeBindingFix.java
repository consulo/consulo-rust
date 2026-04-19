/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsAssocTypeBinding;
import org.rust.lang.core.psi.RsTypeArgumentList;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.lang.core.psi.ext.PsiElementUtil;

public class RemoveAssocTypeBindingFix extends RsQuickFixBase<PsiElement> {

    public RemoveAssocTypeBindingFix(@NotNull PsiElement binding) {
        super(binding);
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.remove.redundant.associated.type");
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull PsiElement element) {
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
