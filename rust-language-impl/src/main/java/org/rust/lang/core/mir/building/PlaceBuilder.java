/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.building;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
    @Nonnull
    private final PlaceBase base;
    @Nonnull
    private final List<PlaceElem> projections;

    public PlaceBuilder(@Nonnull PlaceBase base, @Nonnull List<PlaceElem> projections) {
        this.base = base;
        this.projections = projections;
    }

    public PlaceBuilder(@Nonnull MirLocal local) {
        this(new PlaceBase.Local(local), new ArrayList<>());
    }

    // https://github.com/rust-lang/rust/blob/f7b831ac8a897273f78b9f47165cf8e54066ce4b/compiler/rustc_mir_build/src/build/expr/as_place.rs#L255
    @Nonnull
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

    @Nonnull
    public PlaceBuilder field(int fieldIndex, @Nonnull Ty ty) {
        return project(new MirProjectionElem.Field(fieldIndex, ty));
    }

    @Nonnull
    public PlaceBuilder index(@Nonnull MirLocal index) {
        return project(new MirProjectionElem.Index(index));
    }

    @Nonnull
    public PlaceBuilder project(@Nonnull PlaceElem element) {
        projections.add(element);
        return this;
    }

    @Nonnull
    public PlaceBuilder cloneProject(@Nonnull PlaceElem element) {
        return copy().project(element);
    }

    @Nonnull
    public PlaceBuilder deref() {
        projections.add(MirProjectionElem.Deref.INSTANCE);
        return this;
    }

    @Nonnull
    public PlaceBuilder copy() {
        return new PlaceBuilder(base, new ArrayList<>(projections));
    }

    @Nonnull
    public PlaceBuilder downcast(@Nonnull RsEnumItem item, int variantIndex) {
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
