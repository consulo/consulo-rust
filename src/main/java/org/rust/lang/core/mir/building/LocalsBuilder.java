/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.building;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.mir.schemas.*;
import org.rust.lang.core.types.ty.Mutability;
import org.rust.lang.core.types.ty.Ty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LocalsBuilder {
    @NotNull
    private final List<MirLocal> locals = new ArrayList<>();

    public LocalsBuilder(@NotNull Ty returnTy, @NotNull MirSourceInfo returnSource) {
        allocateReturnLocal(returnTy, returnSource);
    }

    @NotNull
    public MirPlace returnPlace() {
        return new MirPlace(locals.get(0));
    }

    @NotNull
    public MirPlace getReturnPlace() {
        return returnPlace();
    }

    @NotNull
    public MirLocal get(int index) {
        return locals.get(index);
    }

    /**
     * This function will change the object that is stored at the given index.
     */
    public void update(int index, @NotNull Mutability mutability, @NotNull MirSourceInfo source, @NotNull MirLocalInfo localInfo) {
        locals.set(index, locals.get(index).copy(mutability, source, localInfo));
    }

    @NotNull
    public MirLocal newLocal(
        @NotNull Mutability mutability,
        boolean internal,
        @Nullable MirLocalInfo localInfo,
        @Nullable MirBlockTailInfo blockTail,
        @NotNull Ty ty,
        @NotNull MirSourceInfo source
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

    @NotNull
    public MirLocal newLocal(@NotNull Ty ty, @NotNull MirSourceInfo source) {
        return newLocal(Mutability.MUTABLE, false, null, null, ty, source);
    }

    @NotNull
    public MirLocal newLocal(boolean internal, @NotNull Ty ty, @NotNull MirSourceInfo source) {
        return newLocal(Mutability.MUTABLE, internal, null, null, ty, source);
    }

    @NotNull
    public List<MirLocal> build() {
        return Collections.unmodifiableList(new ArrayList<>(locals));
    }

    @NotNull
    private MirLocal allocateReturnLocal(@NotNull Ty ty, @NotNull MirSourceInfo source) {
        return newLocal(Mutability.MUTABLE, false, null, null, ty, source);
    }
}
