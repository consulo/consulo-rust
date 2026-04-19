/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.mir.WithIndex;
import org.rust.lang.core.types.ty.Mutability;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyUnknown;

public class MirLocal implements WithIndex {
    private final int index;
    @NotNull
    private final Mutability mutability;
    private final boolean internal;
    @Nullable
    private final MirLocalInfo localInfo;
    @Nullable
    private final MirBlockTailInfo blockTail;
    @NotNull
    private final Ty ty;
    @NotNull
    private final MirSourceInfo source;

    public MirLocal(
        int index,
        @NotNull Mutability mutability,
        boolean internal,
        @Nullable MirLocalInfo localInfo,
        @Nullable MirBlockTailInfo blockTail,
        @NotNull Ty ty,
        @NotNull MirSourceInfo source
    ) {
        this.index = index;
        this.mutability = mutability;
        this.internal = internal;
        this.localInfo = localInfo;
        this.blockTail = blockTail;
        this.ty = ty;
        this.source = source;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @NotNull
    public Mutability getMutability() {
        return mutability;
    }

    public boolean isInternal() {
        return internal;
    }

    @Nullable
    public MirLocalInfo getLocalInfo() {
        return localInfo;
    }

    @Nullable
    public MirBlockTailInfo getBlockTail() {
        return blockTail;
    }

    @NotNull
    public Ty getTy() {
        return ty;
    }

    @NotNull
    public MirSourceInfo getSource() {
        return source;
    }

    /** Returns true if this is a reference to a thread-local static item that is used to access that static. */
    public boolean isRefToThreadLocal() {
        if (localInfo instanceof MirLocalInfo.StaticRef) {
            return ((MirLocalInfo.StaticRef) localInfo).isThreadLocal();
        }
        return false;
    }

    /** Returns true if this is a reference to a static item that is used to access that static. */
    public boolean isRefToStatic() {
        return localInfo instanceof MirLocalInfo.StaticRef;
    }

    @Override
    public String toString() {
        return "_" + index + ": " + ty;
    }

    @NotNull
    public MirLocal copy(@NotNull Mutability mutability, @NotNull MirSourceInfo source, @NotNull MirLocalInfo localInfo) {
        return new MirLocal(index, mutability, internal, localInfo, blockTail, ty, source);
    }

    @NotNull
    public MirPlace intoPlace() {
        return new MirPlace(this);
    }

    public static final MirLocal fake = new MirLocal(-1, Mutability.MUTABLE, true, null, null, TyUnknown.INSTANCE, MirSourceInfo.fake);
}
