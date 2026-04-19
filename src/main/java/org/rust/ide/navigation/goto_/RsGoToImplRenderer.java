/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.navigation.goto_;

import com.intellij.ide.util.DefaultPsiElementCellRenderer;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.RsAbstractable;
import org.rust.lang.core.psi.ext.RsAbstractableOwner;
import org.rust.lang.core.psi.ext.RsAbstractableImplUtil;

public class RsGoToImplRenderer extends DefaultPsiElementCellRenderer {

    @Override
    public String getElementText(@Nullable PsiElement element) {
        return super.getElementText(getTarget(element));
    }

    @Override
    @Nullable
    public String getContainerText(@Nullable PsiElement element, @Nullable String name) {
        return super.getContainerText(getTarget(element), name);
    }

    @Nullable
    private PsiElement getTarget(@Nullable PsiElement element) {
        if (element instanceof RsAbstractable) {
            RsAbstractableOwner owner = RsAbstractableImplUtil.getOwner((RsAbstractable) element);
            if (owner instanceof RsAbstractableOwner.Impl) {
                return ((RsAbstractableOwner.Impl) owner).getImpl();
            } else if (owner instanceof RsAbstractableOwner.Trait) {
                return ((RsAbstractableOwner.Trait) owner).getTrait();
            } else {
                return element;
            }
        }
        return element;
    }
}
