/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.PsiElement;
import org.rust.lang.core.psi.RsPsiUtilUtil;
import org.rust.lang.core.resolve.ref.RsReference;

/**
 * Marks an element that has a reference.
 */
public interface RsMandatoryReferenceElement extends RsReferenceElement {
    @Override
    PsiElement getReferenceNameElement();

    @Override
    default String getReferenceName() {
        return RsPsiUtilUtil.getUnescapedText(getReferenceNameElement());
    }

    @Override
    RsReference getReference();
}
