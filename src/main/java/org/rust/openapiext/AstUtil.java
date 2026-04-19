/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import com.intellij.lang.ASTNode;
import com.intellij.lang.FileASTNode;
import org.jetbrains.annotations.NotNull;

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
    public static void forEachChild(@NotNull ASTNode node, @NotNull Consumer<ASTNode> action) {
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
    @NotNull
    public static Iterable<ASTNode> ancestors(@NotNull ASTNode node) {
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
    @NotNull
    public static List<ASTNode> ancestorsList(@NotNull ASTNode node) {
        List<ASTNode> result = new ArrayList<>();
        for (ASTNode ancestor : ancestors(node)) {
            result.add(ancestor);
        }
        return result;
    }
}
