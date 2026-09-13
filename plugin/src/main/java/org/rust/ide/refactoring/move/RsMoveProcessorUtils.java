/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.move;

import org.rust.lang.core.psi.ext.impl.RsElementUtil;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.impl.RsModUtil;

import java.util.List;
import org.rust.lang.core.psi.impl.*;

public final class RsMoveProcessorUtils {

    private RsMoveProcessorUtils() {
    }

    public static void insertModDecl(@Nonnull RsMod mod, @Nonnull RsPsiFactory psiFactory, @Nonnull PsiElement modDecl) {
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
