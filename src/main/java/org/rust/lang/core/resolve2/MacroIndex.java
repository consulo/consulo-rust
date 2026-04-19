/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * We give macro index to each macro def, macro call, and mod.
 */
public final class MacroIndex implements Comparable<MacroIndex> {

    @NotNull
    private final int[] indices;

    public MacroIndex(@NotNull int[] indices) {
        this.indices = indices;
    }

    @NotNull
    public int[] getIndices() {
        return indices;
    }

    @NotNull
    public MacroIndex getParent() {
        return new MacroIndex(Arrays.copyOfRange(indices, 0, indices.length - 1));
    }

    public int getLast() {
        return indices[indices.length - 1];
    }

    @NotNull
    public MacroIndex append(int index) {
        int[] newIndices = Arrays.copyOf(indices, indices.length + 1);
        newIndices[indices.length] = index;
        return new MacroIndex(newIndices);
    }

    @Override
    public int compareTo(@NotNull MacroIndex other) {
        return Arrays.compare(indices, other.indices);
    }

    /** Equivalent to {@code call < mod && !isPrefix(call, mod)} */
    public static boolean shouldPropagate(@NotNull MacroIndex call, @NotNull MacroIndex mod) {
        int[] callIndices = call.indices;
        int[] modIndices = mod.indices;
        int commonPrefix = Arrays.mismatch(callIndices, modIndices);
        return commonPrefix != callIndices.length
            && commonPrefix != modIndices.length
            && callIndices[commonPrefix] < modIndices[commonPrefix];
    }

    public static boolean equals(@NotNull MacroIndex index1, @NotNull MacroIndex index2) {
        return Arrays.equals(index1.indices, index2.indices);
    }

    public static int hashCode(@NotNull MacroIndex index) {
        return Arrays.hashCode(index.indices);
    }

    @Override
    @NotNull
    public String toString() {
        return Arrays.toString(indices);
    }
}
