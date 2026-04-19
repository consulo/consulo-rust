/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.RsTypeReference;

public class ConvertToBoxFix extends ConvertToSizedTypeFix {

    public ConvertToBoxFix(@NotNull PsiElement element) {
        super(element);
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.convert.to.box");
    }

    @NotNull
    @Override
    protected RsTypeReference newTypeReference(@NotNull RsPsiFactory factory, @NotNull RsTypeReference typeReference) {
        return factory.createType("Box<" + typeReference.getText() + ">");
    }
}
