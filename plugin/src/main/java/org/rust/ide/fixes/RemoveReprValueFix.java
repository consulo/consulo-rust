/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;

public class RemoveReprValueFix extends RemoveElementFix {

    public RemoveReprValueFix(@Nonnull PsiElement metaItem) {
        super(metaItem);
    }
}
