/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import consulo.language.ast.ASTNode;
import consulo.language.impl.ast.RecursiveTreeElementWalkingVisitor;
import consulo.language.impl.ast.TreeElement;
import consulo.language.ast.IElementType;
import jakarta.annotation.Nonnull;

import static org.rust.lang.core.psi.RsElementTypes.MACRO;
import static org.rust.lang.core.psi.RsTokenType.RS_ITEMS;

final class ItemSeekingVisitor extends RecursiveTreeElementWalkingVisitor {
    private boolean hasItemsOrAttrs = false;

    private ItemSeekingVisitor() {
    }

    @Override
    protected void visitNode(@Nonnull TreeElement element) {
        IElementType elementType = element.getElementType();
        if (RS_ITEMS.contains(elementType) || elementType == MACRO) {
            hasItemsOrAttrs = true;
            stopWalking();
        } else {
            super.visitNode(element);
        }
    }

    static boolean containsItems(@Nonnull ASTNode node) {
        ItemSeekingVisitor visitor = new ItemSeekingVisitor();
        ((TreeElement) node).acceptTree(visitor);
        return visitor.hasItemsOrAttrs;
    }
}
