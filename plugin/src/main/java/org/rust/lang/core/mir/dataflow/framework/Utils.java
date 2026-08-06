/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.dataflow.framework;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.mir.WithIndex;
import org.rust.lang.core.mir.schemas.MirBasicBlock;
import org.rust.lang.core.mir.schemas.MirBody;
import org.rust.stdext.StdextUtil;

import java.util.*;

public final class Utils {
    private Utils() {
    }

    @Nonnull
    public static List<MirBasicBlock> getBasicBlocksInPostOrder(@Nonnull MirBody body) {
        return getBasicBlocksInOrder(body, true);
    }

    @Nonnull
    public static List<MirBasicBlock> getBasicBlocksInPreOrder(@Nonnull MirBody body) {
        return getBasicBlocksInOrder(body, false);
    }

    @Nonnull
    private static List<MirBasicBlock> getBasicBlocksInOrder(@Nonnull MirBody body, boolean postOrder) {
        BitSet visited = new BitSet(body.getBasicBlocks().size());
        ArrayDeque<Map.Entry<MirBasicBlock, Iterator<MirBasicBlock>>> queue = new ArrayDeque<>();
        List<MirBasicBlock> result = new ArrayList<>();

        MirBasicBlock first = body.getBasicBlocks().get(0);
        if (addToVisited(visited, first.getIndex())) {
            queue.push(new AbstractMap.SimpleEntry<>(first, first.getTerminator().getSuccessors().iterator()));
        }

        while (!queue.isEmpty()) {
            Map.Entry<MirBasicBlock, Iterator<MirBasicBlock>> entry;
            if (postOrder) {
                entry = queue.removeFirst();
            } else {
                entry = queue.removeLast();
            }

            MirBasicBlock node = entry.getKey();
            Iterator<MirBasicBlock> iterator = entry.getValue();
            MirBasicBlock target = StdextUtil.nextOrNull(iterator);
            if (target != null) {
                queue.push(new AbstractMap.SimpleEntry<>(node, iterator));
                if (addToVisited(visited, target.getIndex())) {
                    queue.push(new AbstractMap.SimpleEntry<>(target, target.getTerminator().getSuccessors().iterator()));
                }
            } else {
                result.add(node);
            }
        }
        return result;
    }

    public static boolean addToVisited(@Nonnull BitSet visited, int element) {
        if (visited.get(element)) {
            return false;
        } else {
            visited.set(element);
            return true;
        }
    }
}
