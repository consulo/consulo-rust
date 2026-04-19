/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.building;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.mir.schemas.impls.MirBasicBlockImpl;

import java.util.*;

public class DropTree {
    @NotNull
    private final DropNode root = new DropNode.Root();
    @NotNull
    private final Map<DropNode, List<MirBasicBlockImpl>> entryPoints = new HashMap<>();

    @NotNull
    public DropNode getRoot() {
        return root;
    }

    public void addEntry(@NotNull MirBasicBlockImpl from, @NotNull DropNode to) {
        List<MirBasicBlockImpl> list = entryPoints.computeIfAbsent(to, k -> new ArrayList<>());
        list.add(from);
    }

    @NotNull
    public Map<DropNode, MirBasicBlockImpl> buildMir(
        @NotNull DropTreeBuilder dropTreeBuilder,
        @Nullable MirBasicBlockImpl first
    ) {
        Map<DropNode, MirBasicBlockImpl> blocks = new HashMap<>();
        if (first != null) {
            blocks.put(root, first);
        }
        assignBlocks(dropTreeBuilder, blocks);
        // TODO: linkDrops. It will be needed soon I guess
        return blocks;
    }

    private void assignBlocks(
        @NotNull DropTreeBuilder dropTreeBuilder,
        @NotNull Map<DropNode, MirBasicBlockImpl> blocks
    ) {
        for (DropNode drop : root) {
            List<MirBasicBlockImpl> dropBlocks = entryPoints.get(drop);
            if (dropBlocks != null) {
                MirBasicBlockImpl block = blocks.computeIfAbsent(drop, k -> dropTreeBuilder.makeBlock());
                List<MirBasicBlockImpl> reversed = new ArrayList<>(dropBlocks);
                Collections.reverse(reversed);
                for (MirBasicBlockImpl entryBlock : reversed) {
                    dropTreeBuilder.addEntry(entryBlock, block);
                }
            }
            // TODO: there is more
        }
    }

    @NotNull
    public DropNode addDrop(@NotNull Drop drop, @NotNull DropNode next) {
        DropNode node = new DropNode.Default(next, drop);
        next.getPrevious().add(node);
        return node;
    }

    /**
     * Iterates over itself and its previous drops.
     */
    public static abstract class DropNode implements Iterable<DropNode> {
        @NotNull
        private final List<DropNode> previous = new ArrayList<>();

        @NotNull
        public List<DropNode> getPrevious() {
            return previous;
        }

        @Override
        @NotNull
        public Iterator<DropNode> iterator() {
            List<DropNode> result = new ArrayList<>();
            result.add(this);
            for (DropNode prev : previous) {
                for (DropNode innerPrev : prev.previous) {
                    result.add(innerPrev);
                }
            }
            return result.iterator();
        }

        public static final class Root extends DropNode {
        }

        public static final class Default extends DropNode {
            @NotNull
            private final DropNode next;
            @NotNull
            private final Drop drop;

            public Default(@NotNull DropNode next, @NotNull Drop drop) {
                this.next = next;
                this.drop = drop;
            }

            @NotNull
            public DropNode getNext() {
                return next;
            }

            @NotNull
            public Drop getDrop() {
                return drop;
            }
        }
    }
}
