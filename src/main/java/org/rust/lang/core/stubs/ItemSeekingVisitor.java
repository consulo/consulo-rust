/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import com.intellij.lang.ASTNode;
import com.intellij.psi.impl.source.tree.RecursiveTreeElementWalkingVisitor;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

import static org.rust.lang.core.psi.RsElementTypes.MACRO;
import static org.rust.lang.core.psi.RsTokenType.RS_ITEMS;

final class ItemSeekingVisitor extends RecursiveTreeElementWalkingVisitor {
    private boolean hasItemsOrAttrs = false;

    private ItemSeekingVisitor() {
    }

    @Override
    protected void visitNode(@NotNull TreeElement element) {
        IElementType elementType = element.getElementType();
        if (RS_ITEMS.contains(elementType) || elementType == MACRO) {
            hasItemsOrAttrs = true;
            stopWalking();
        } else {
            super.visitNode(element);
        }
    }

    static boolean containsItems(@NotNull ASTNode node) {
        ItemSeekingVisitor visitor = new ItemSeekingVisitor();
        ((TreeElement) node).acceptTree(visitor);
        return visitor.hasItemsOrAttrs;
    }
}
