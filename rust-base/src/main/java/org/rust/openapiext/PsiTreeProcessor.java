/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;

@FunctionalInterface
public interface PsiTreeProcessor {
    @Nonnull
    TreeStatus execute(@Nonnull PsiElement element);
}
