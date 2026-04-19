/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public class RemoveReprValueFix extends RemoveElementFix {

    public RemoveReprValueFix(@NotNull PsiElement metaItem) {
        super(metaItem);
    }
}
