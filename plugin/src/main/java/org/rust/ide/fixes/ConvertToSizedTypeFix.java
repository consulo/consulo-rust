/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.RsTypeReference;

public abstract class ConvertToSizedTypeFix extends RsQuickFixBase<PsiElement> {

    public ConvertToSizedTypeFix(@Nonnull PsiElement element) {
        super(element);
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.convert.to.sized.type"));
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull PsiElement element) {
        if (!(element instanceof RsTypeReference)) return;
        RsPsiFactory factory = new RsPsiFactory(project);
        RsTypeReference newTypeReference = newTypeReference(factory, (RsTypeReference) element);
        element.replace(newTypeReference);
    }

    @Nonnull
    protected abstract RsTypeReference newTypeReference(@Nonnull RsPsiFactory factory, @Nonnull RsTypeReference typeReference);
}
