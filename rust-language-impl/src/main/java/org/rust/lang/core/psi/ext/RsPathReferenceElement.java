/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nullable;
import org.rust.lang.core.resolve.ref.RsPathReference;
import org.rust.lang.core.stubs.common.RsPathPsiOrStub;
import org.rust.lang.core.psi.impl.RsPsiUtilUtil;

public interface RsPathReferenceElement extends RsReferenceElement, RsPathPsiOrStub {
    @Override
    @Nullable
    RsPathReference getReference();

    @Override
    @Nullable
    PsiElement getReferenceNameElement();

    @Override
    @Nullable
    default String getReferenceName() {
        PsiElement element = getReferenceNameElement();
        return element != null ? org.rust.lang.core.psi.impl.RsPsiUtilUtil.getUnescapedText(element) : null;
    }
}
