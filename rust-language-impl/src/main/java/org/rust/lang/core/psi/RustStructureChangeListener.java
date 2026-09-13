/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nullable;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;
import consulo.annotation.component.TopicBroadcastDirection;

@TopicAPI(value = ComponentScope.PROJECT, direction = TopicBroadcastDirection.TO_PARENT)
public interface RustStructureChangeListener {
    void rustStructureChanged(@Nullable PsiFile file, @Nullable PsiElement changedElement);
}
