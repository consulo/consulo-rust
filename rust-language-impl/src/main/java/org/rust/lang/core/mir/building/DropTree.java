/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.building;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.mir.schemas.impls.MirBasicBlockImpl;

import java.util.*;

public class DropTree {
    @Nonnull
    private final DropNode root = new DropNode.Root();
    @Nonnull
    private final Map<DropNode, List<MirBasicBlockImpl>> entryPoints = new HashMap<>();

    @Nonnull
    public DropNode getRoot() {
        return root;
    }

    public void addEntry(@Nonnull MirBasicBlockImpl from, @Nonnull DropNode to) {
        List<MirBasicBlockImpl> list = entryPoints.computeIfAbsent(to, k -> new ArrayList<>());
        list.add(from);
    }

    @Nonnull
    public Map<DropNode, MirBasicBlockImpl> buildMir(
        @Nonnull DropTreeBuilder dropTreeBuilder,
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
        @Nonnull DropTreeBuilder dropTreeBuilder,
        @Nonnull Map<DropNode, MirBasicBlockImpl> blocks
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

    @Nonnull
    public DropNode addDrop(@Nonnull Drop drop, @Nonnull DropNode next) {
        DropNode node = new DropNode.Default(next, drop);
        next.getPrevious().add(node);
        return node;
    }

    /**
     * Iterates over itself and its previous drops.
     */
    public static abstract class DropNode implements Iterable<DropNode> {
        @Nonnull
        private final List<DropNode> previous = new ArrayList<>();

        @Nonnull
        public List<DropNode> getPrevious() {
            return previous;
        }

        @Override
        @Nonnull
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
            @Nonnull
            private final DropNode next;
            @Nonnull
            private final Drop drop;

            public Default(@Nonnull DropNode next, @Nonnull Drop drop) {
                this.next = next;
                this.drop = drop;
            }

            @Nonnull
            public DropNode getNext() {
                return next;
            }

            @Nonnull
            public Drop getDrop() {
                return drop;
            }
        }
    }
}
