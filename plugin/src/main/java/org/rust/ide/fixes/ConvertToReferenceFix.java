/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.RsTypeReference;

public class ConvertToReferenceFix extends ConvertToSizedTypeFix {

    public ConvertToReferenceFix(@Nonnull PsiElement element) {
        super(element);
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.convert.to.reference"));
    }

    @Nonnull
    @Override
    protected RsTypeReference newTypeReference(@Nonnull RsPsiFactory factory, @Nonnull RsTypeReference typeReference) {
        return factory.createType("&" + typeReference.getText());
    }
}
