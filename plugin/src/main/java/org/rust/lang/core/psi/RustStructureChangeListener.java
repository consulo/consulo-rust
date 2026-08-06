/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nullable;

public interface RustStructureChangeListener {
    void rustStructureChanged(@Nullable PsiFile file, @Nullable PsiElement changedElement);
}
