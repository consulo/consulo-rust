/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.navigation.goto_;

import consulo.ide.impl.idea.codeInsight.navigation.GotoTargetHandler;
import consulo.ide.navigation.GotoTargetRendererProvider;
import consulo.language.editor.ui.PsiElementListCellRenderer;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsImplItem;
import org.rust.lang.core.psi.ext.RsAbstractable;

@SuppressWarnings("deprecation")
public class RsGotoTargetRendererProvider implements GotoTargetRendererProvider {
    @Nullable
    public PsiElementListCellRenderer<?> getRenderer(
        @Nonnull PsiElement element,
        @Nonnull GotoTargetHandler.GotoData gotoData
    ) {
        if (element instanceof RsImplItem || (element instanceof RsAbstractable && !gotoData.hasDifferentNames())) {
            return new RsGoToImplRenderer();
        } else {
            return null;
        }
    }
}
