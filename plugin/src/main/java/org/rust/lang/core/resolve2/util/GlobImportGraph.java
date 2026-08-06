/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2.util;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.resolve2.ModData;
import org.rust.lang.core.resolve2.Visibility;
import org.rust.lang.utils.Direction;
import org.rust.lang.utils.Graph;
import org.rust.lang.utils.Node;
import org.rust.lang.utils.Edge;

import java.util.*;

public class GlobImportGraph {
    @Nonnull
    private final Graph<ModData, Visibility> graph = new Graph<>();
    @Nonnull
    private final Map<ModData, Integer> modDataToIndex = new HashMap<>();

    private int getIndex(@Nonnull ModData modData) {
        return modDataToIndex.computeIfAbsent(modData, md -> graph.addNode(md).getIndex());
    }

    public void recordGlobImport(@Nonnull ModData source, @Nonnull ModData target, @Nonnull Visibility visibility) {
        if (visibility.isInvisible()) return;
        int sourceIndex = getIndex(source);
        int targetIndex = getIndex(target);
        graph.addEdge(sourceIndex, targetIndex, visibility);
    }

    public boolean hasTransitiveGlobImport(@Nonnull ModData source, @Nonnull ModData target) {
        Integer sourceIndex = modDataToIndex.get(source);
        if (sourceIndex == null) return false;
        Node<ModData, Visibility> sourceNode = graph.getNode(sourceIndex);
        Iterable<Node<ModData, Visibility>> accessibleMods = graph.depthFirstTraversal(
            sourceNode, Direction.OUTGOING, edge -> edge.getData().isVisibleFromMod(source)
        );
        for (Node<ModData, Visibility> mod : accessibleMods) {
            if (mod.getData() == target) return true;
        }
        return false;
    }
}
