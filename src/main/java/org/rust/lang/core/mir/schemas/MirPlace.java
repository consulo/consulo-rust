/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.types.ty.Ty;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class MirPlace {
    @NotNull
    private final MirLocal local;
    @NotNull
    private final List<MirProjectionElem<Ty>> projections;

    public MirPlace(@NotNull MirLocal local) {
        this(local, Collections.emptyList());
    }

    public MirPlace(@NotNull MirLocal local, @NotNull List<MirProjectionElem<Ty>> projections) {
        this.local = local;
        this.projections = projections;
    }

    @NotNull
    public MirLocal getLocal() {
        return local;
    }

    @NotNull
    public List<MirProjectionElem<Ty>> getProjections() {
        return projections;
    }

    @NotNull
    public MirPlace makeField(int fieldIndex, @NotNull Ty ty) {
        List<MirProjectionElem<Ty>> newProjections = new ArrayList<>(projections);
        newProjections.add(new MirProjectionElem.Field<>(fieldIndex, ty));
        return new MirPlace(local, newProjections);
    }

    // https://github.com/rust-lang/rust/blob/f7b831ac8a897273f78b9f47165cf8e54066ce4b/compiler/rustc_middle/src/mir/tcx.rs#L135
    @NotNull
    public MirPlaceTy ty() {
        return tyFrom(local, projections);
    }

    // https://github.com/rust-lang/rust/blob/f7b831ac8a897273f78b9f47165cf8e54066ce4b/compiler/rustc_middle/src/mir/tcx.rs#L119
    @NotNull
    public static MirPlaceTy tyFrom(@NotNull MirLocal local, @NotNull List<MirProjectionElem<Ty>> projections) {
        MirPlaceTy placeTy = MirPlaceTy.fromTy(local.getTy());
        for (MirProjectionElem<Ty> element : projections) {
            placeTy = placeTy.projectionTy(element);
        }
        return placeTy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MirPlace mirPlace = (MirPlace) o;
        return Objects.equals(local, mirPlace.local) && Objects.equals(projections, mirPlace.projections);
    }

    @Override
    public int hashCode() {
        return Objects.hash(local, projections);
    }

    @Override
    public String toString() {
        return "MirPlace(local=" + local + ", projections=" + projections + ")";
    }
}
