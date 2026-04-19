/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface PsiTreeProcessor {
    @NotNull
    TreeStatus execute(@NotNull PsiElement element);
}
