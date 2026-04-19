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
import org.rust.lang.core.psi.RsTypeReference;

public abstract class ConvertToSizedTypeFix extends RsQuickFixBase<PsiElement> {

    public ConvertToSizedTypeFix(@NotNull PsiElement element) {
        super(element);
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.convert.to.sized.type");
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull PsiElement element) {
        if (!(element instanceof RsTypeReference)) return;
        RsPsiFactory factory = new RsPsiFactory(project);
        RsTypeReference newTypeReference = newTypeReference(factory, (RsTypeReference) element);
        element.replace(newTypeReference);
    }

    @NotNull
    protected abstract RsTypeReference newTypeReference(@NotNull RsPsiFactory factory, @NotNull RsTypeReference typeReference);
}
