/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.PsiElement;
import org.rust.lang.core.psi.RsPsiUtilUtil;

/**
 * Provides basic methods for reference implementation.
 * This interface should not be used in any analysis.
 */
public interface RsReferenceElementBase extends RsElement {
    PsiElement getReferenceNameElement();

    default String getReferenceName() {
        PsiElement element = getReferenceNameElement();
        return element != null ? RsPsiUtilUtil.getUnescapedText(element) : null;
    }
}
