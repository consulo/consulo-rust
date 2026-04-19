/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.building;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.mir.schemas.impls.MirBasicBlockImpl;

import java.util.Objects;
import java.util.function.Function;

public class BlockAnd<T> {
    @NotNull
    private final MirBasicBlockImpl block;
    @Nullable
    private final T elem;

    public BlockAnd(@NotNull MirBasicBlockImpl block, @Nullable T elem) {
        this.block = block;
        this.elem = elem;
    }

    @NotNull
    public static <T> BlockAnd<T> of(@NotNull MirBasicBlockImpl block, @Nullable T elem) {
        return new BlockAnd<>(block, elem);
    }

    @NotNull
    public static BlockAnd<Void> andUnit(@NotNull MirBasicBlockImpl block) {
        return new BlockAnd<>(block, null);
    }

    /**
     * Instance method that creates a BlockAnd&lt;Void&gt; from the current block,
     */
    @NotNull
    public BlockAnd<Void> andUnit() {
        return new BlockAnd<>(block, null);
    }

    @NotNull
    public MirBasicBlockImpl getBlock() {
        return block;
    }

    @Nullable
    public T getElem() {
        return elem;
    }

    @NotNull
    public <R> BlockAnd<R> map(@NotNull Function<T, R> transform) {
        return new BlockAnd<>(block, transform.apply(elem));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockAnd<?> blockAnd = (BlockAnd<?>) o;
        return Objects.equals(block, blockAnd.block) && Objects.equals(elem, blockAnd.elem);
    }

    @Override
    public int hashCode() {
        return Objects.hash(block, elem);
    }

    @Override
    public String toString() {
        return "BlockAnd(block=" + block + ", elem=" + elem + ")";
    }
}
