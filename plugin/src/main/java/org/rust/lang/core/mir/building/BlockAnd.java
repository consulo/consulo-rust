/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.building;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.mir.schemas.impls.MirBasicBlockImpl;

import java.util.Objects;
import java.util.function.Function;

public class BlockAnd<T> {
    @Nonnull
    private final MirBasicBlockImpl block;
    @Nullable
    private final T elem;

    public BlockAnd(@Nonnull MirBasicBlockImpl block, @Nullable T elem) {
        this.block = block;
        this.elem = elem;
    }

    @Nonnull
    public static <T> BlockAnd<T> of(@Nonnull MirBasicBlockImpl block, @Nullable T elem) {
        return new BlockAnd<>(block, elem);
    }

    @Nonnull
    public static BlockAnd<Void> andUnit(@Nonnull MirBasicBlockImpl block) {
        return new BlockAnd<>(block, null);
    }

    /**
     * Instance method that creates a BlockAnd&lt;Void&gt; from the current block,
     */
    @Nonnull
    public BlockAnd<Void> andUnit() {
        return new BlockAnd<>(block, null);
    }

    @Nonnull
    public MirBasicBlockImpl getBlock() {
        return block;
    }

    @Nullable
    public T getElem() {
        return elem;
    }

    @Nonnull
    public <R> BlockAnd<R> map(@Nonnull Function<T, R> transform) {
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
