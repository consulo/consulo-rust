/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.move;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.RsModUtil;

import java.util.List;

public final class RsMoveProcessorUtils {

    private RsMoveProcessorUtils() {
    }

    public static void insertModDecl(@NotNull RsMod mod, @NotNull RsPsiFactory psiFactory, @NotNull PsiElement modDecl) {
        List<RsModDeclItem> modDeclItems = RsElementUtil.childrenOfType(mod, RsModDeclItem.class);
        List<RsUseItem> useItems = RsElementUtil.childrenOfType(mod, RsUseItem.class);

        PsiElement anchor = null;
        if (!modDeclItems.isEmpty()) {
            anchor = modDeclItems.get(modDeclItems.size() - 1);
        } else if (!useItems.isEmpty()) {
            anchor = useItems.get(useItems.size() - 1);
        }

        if (anchor != null) {
            mod.addAfter(modDecl, anchor);
        } else {
            PsiElement firstItem = RsModUtil.getFirstItem(mod);
            if (firstItem == null && mod instanceof RsModItem) {
                firstItem = ((RsModItem) mod).getRbrace();
            }
            mod.addBefore(modDecl, firstItem);
        }

        if (modDecl.getNextSibling() == null) {
            mod.addAfter(psiFactory.createNewline(), modDecl);
        }
    }
}
