/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.dataflow.framework;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.mir.WithIndex;

import java.util.ArrayDeque;
import java.util.BitSet;

public class WorkQueue<T extends WithIndex> {
    @NotNull
    private final ArrayDeque<T> deque;
    @NotNull
    private final BitSet set;

    public WorkQueue(int size) {
        this.deque = new ArrayDeque<>(size);
        this.set = new BitSet(size);
    }

    public void insert(@NotNull T element) {
        if (Utils.addToVisited(set, element.getIndex())) {
            deque.push(element);
        }
    }

    @NotNull
    public T pop() {
        T element = deque.pop();
        set.set(element.getIndex(), false);
        return element;
    }

    public boolean isEmpty() {
        return deque.isEmpty();
    }
}
