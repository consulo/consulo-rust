/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import consulo.language.ast.ASTNode;
import consulo.language.ast.FileASTNode;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Utility methods for working with AST nodes.
 */
public final class AstUtil {
    private AstUtil() {
    }

    /**
     * Iterates all children of the given node and invokes the action for each one.
     */
    public static void forEachChild(@Nonnull ASTNode node, @Nonnull Consumer<ASTNode> action) {
        ASTNode treeChild = node.getFirstChildNode();
        while (treeChild != null) {
            action.accept(treeChild);
            treeChild = treeChild.getTreeNext();
        }
    }

    /**
     * Returns an iterable of ancestors of the given node (including the node itself),
     * stopping at the file node.
     */
    @Nonnull
    public static Iterable<ASTNode> ancestors(@Nonnull ASTNode node) {
        return () -> new Iterator<ASTNode>() {
            private ASTNode myCurrent = node;

            @Override
            public boolean hasNext() {
                return myCurrent != null;
            }

            @Override
            public ASTNode next() {
                ASTNode result = myCurrent;
                if (myCurrent instanceof FileASTNode) {
                    myCurrent = null;
                } else {
                    myCurrent = myCurrent.getTreeParent();
                }
                return result;
            }
        };
    }

    /**
     * Collects all ancestors into a list.
     */
    @Nonnull
    public static List<ASTNode> ancestorsList(@Nonnull ASTNode node) {
        List<ASTNode> result = new ArrayList<>();
        for (ASTNode ancestor : ancestors(node)) {
            result.add(ancestor);
        }
        return result;
    }
}
