/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.building;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.mir.schemas.MirLocal;
import org.rust.lang.core.mir.schemas.MirPlace;
import org.rust.lang.core.mir.schemas.MirProjectionElem;
import org.rust.lang.core.mir.schemas.PlaceElem;
import org.rust.lang.core.psi.RsEnumItem;
import org.rust.lang.core.thir.ThirUtilUtil;
import org.rust.lang.core.types.ty.Ty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PlaceBuilder {
    @NotNull
    private final PlaceBase base;
    @NotNull
    private final List<PlaceElem> projections;

    public PlaceBuilder(@NotNull PlaceBase base, @NotNull List<PlaceElem> projections) {
        this.base = base;
        this.projections = projections;
    }

    public PlaceBuilder(@NotNull MirLocal local) {
        this(new PlaceBase.Local(local), new ArrayList<>());
    }

    // https://github.com/rust-lang/rust/blob/f7b831ac8a897273f78b9f47165cf8e54066ce4b/compiler/rustc_mir_build/src/build/expr/as_place.rs#L255
    @NotNull
    public MirPlace toPlace() {
        MirPlace place = tryToPlace();
        if (place == null) {
            throw new IllegalStateException("Cannot convert PlaceBuilder to MirPlace");
        }
        return place;
    }

    @Nullable
    public MirPlace tryToPlace() {
        if (base instanceof PlaceBase.Local) {
            @SuppressWarnings("unchecked")
            List<MirProjectionElem<Ty>> mirProjections = (List<MirProjectionElem<Ty>>) (List<?>) new ArrayList<>(projections);
            return new MirPlace(((PlaceBase.Local) base).getLocal(), mirProjections);
        }
        return null;
    }

    @NotNull
    public PlaceBuilder field(int fieldIndex, @NotNull Ty ty) {
        return project(new MirProjectionElem.Field(fieldIndex, ty));
    }

    @NotNull
    public PlaceBuilder index(@NotNull MirLocal index) {
        return project(new MirProjectionElem.Index(index));
    }

    @NotNull
    public PlaceBuilder project(@NotNull PlaceElem element) {
        projections.add(element);
        return this;
    }

    @NotNull
    public PlaceBuilder cloneProject(@NotNull PlaceElem element) {
        return copy().project(element);
    }

    @NotNull
    public PlaceBuilder deref() {
        projections.add(MirProjectionElem.Deref.INSTANCE);
        return this;
    }

    @NotNull
    public PlaceBuilder copy() {
        return new PlaceBuilder(base, new ArrayList<>(projections));
    }

    @NotNull
    public PlaceBuilder downcast(@NotNull RsEnumItem item, int variantIndex) {
        return project(new MirProjectionElem.Downcast(
            ThirUtilUtil.variant(item, variantIndex).getName(),
            variantIndex
        ));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlaceBuilder that = (PlaceBuilder) o;
        return Objects.equals(base, that.base) && Objects.equals(projections, that.projections);
    }

    @Override
    public int hashCode() {
        return Objects.hash(base, projections);
    }

    @Override
    public String toString() {
        return "PlaceBuilder(base=" + base + ", projections=" + projections + ")";
    }
}
