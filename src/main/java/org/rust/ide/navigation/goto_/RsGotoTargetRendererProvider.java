/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.navigation.goto_;

import com.intellij.codeInsight.navigation.GotoTargetHandler;
import com.intellij.codeInsight.navigation.GotoTargetRendererProvider;
import com.intellij.ide.util.PsiElementListCellRenderer;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsImplItem;
import org.rust.lang.core.psi.ext.RsAbstractable;

@SuppressWarnings("deprecation")
public class RsGotoTargetRendererProvider implements GotoTargetRendererProvider {
    @Override
    @Nullable
    public PsiElementListCellRenderer<?> getRenderer(
        @NotNull PsiElement element,
        @NotNull GotoTargetHandler.GotoData gotoData
    ) {
        if (element instanceof RsImplItem || (element instanceof RsAbstractable && !gotoData.hasDifferentNames())) {
            return new RsGoToImplRenderer();
        } else {
            return null;
        }
    }
}
