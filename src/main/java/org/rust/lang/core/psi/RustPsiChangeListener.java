/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public interface RustPsiChangeListener {
    void rustPsiChanged(@NotNull PsiFile file, @NotNull PsiElement element, boolean isStructureModification);
}
