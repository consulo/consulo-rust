/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.building;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.mir.schemas.*;
import org.rust.lang.core.types.ty.Mutability;
import org.rust.lang.core.types.ty.Ty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LocalsBuilder {
    @Nonnull
    private final List<MirLocal> locals = new ArrayList<>();

    public LocalsBuilder(@Nonnull Ty returnTy, @Nonnull MirSourceInfo returnSource) {
        allocateReturnLocal(returnTy, returnSource);
    }

    @Nonnull
    public MirPlace returnPlace() {
        return new MirPlace(locals.get(0));
    }

    @Nonnull
    public MirPlace getReturnPlace() {
        return returnPlace();
    }

    @Nonnull
    public MirLocal get(int index) {
        return locals.get(index);
    }

    /**
     * This function will change the object that is stored at the given index.
     */
    public void update(int index, @Nonnull Mutability mutability, @Nonnull MirSourceInfo source, @Nonnull MirLocalInfo localInfo) {
        locals.set(index, locals.get(index).copy(mutability, source, localInfo));
    }

    @Nonnull
    public MirLocal newLocal(
        @Nonnull Mutability mutability,
        boolean internal,
        @Nullable MirLocalInfo localInfo,
        @Nullable MirBlockTailInfo blockTail,
        @Nonnull Ty ty,
        @Nonnull MirSourceInfo source
    ) {
        MirLocal local = new MirLocal(
            locals.size(),
            mutability,
            internal,
            localInfo,
            blockTail,
            ty,
            source
        );
        locals.add(local);
        return local;
    }

    @Nonnull
    public MirLocal newLocal(@Nonnull Ty ty, @Nonnull MirSourceInfo source) {
        return newLocal(Mutability.MUTABLE, false, null, null, ty, source);
    }

    @Nonnull
    public MirLocal newLocal(boolean internal, @Nonnull Ty ty, @Nonnull MirSourceInfo source) {
        return newLocal(Mutability.MUTABLE, internal, null, null, ty, source);
    }

    @Nonnull
    public List<MirLocal> build() {
        return Collections.unmodifiableList(new ArrayList<>(locals));
    }

    @Nonnull
    private MirLocal allocateReturnLocal(@Nonnull Ty ty, @Nonnull MirSourceInfo source) {
        return newLocal(Mutability.MUTABLE, false, null, null, ty, source);
    }
}
