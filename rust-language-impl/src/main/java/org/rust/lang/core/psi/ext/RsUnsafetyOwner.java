/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nullable;

public interface RsUnsafetyOwner {
    @Nullable
    PsiElement getUnsafe();

    boolean isUnsafe();
}
