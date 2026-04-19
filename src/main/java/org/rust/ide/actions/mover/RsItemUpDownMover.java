/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions.mover;

import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.TokenSet;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsMembers;
import org.rust.lang.core.psi.ext.PsiElementUtil;
import org.rust.lang.core.psi.ext.RsElement;

public class RsItemUpDownMover extends RsLineMover {
    private static final TokenSet movableItems = TokenSet.create(
        RsElementTypes.TRAIT_ITEM,
        RsElementTypes.IMPL_ITEM,
        RsElementTypes.MACRO_CALL,
        RsElementTypes.MACRO,
        RsElementTypes.STRUCT_ITEM,
        RsElementTypes.ENUM_ITEM,
        RsElementTypes.MOD_ITEM,
        RsElementTypes.USE_ITEM,
        RsElementTypes.FUNCTION,
        RsElementTypes.CONSTANT,
        RsElementTypes.TYPE_ALIAS
    );

    @Override
    protected PsiElement findMovableAncestor(PsiElement psi, RangeEndpoint endpoint) {
        PsiElement current = psi;
        while (current != null) {
            if (movableItems.contains(PsiElementUtil.getElementType(current))) return current;
            current = current.getParent();
        }
        return null;
    }

    @Override
    protected PsiElement findTargetElement(PsiElement sibling, boolean down) {
        if (isMovingOutOfBraceBlock(sibling, down) && sibling.getParent() instanceof RsMembers) {
            UpDownMoverTestMarks.MoveOutOfImpl.hit();
            return null;
        }
        return sibling;
    }
}
