/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyUtilUtil;

public class MirPlaceTy {
    @Nonnull
    private final Ty ty;
    /** Downcast to a particular variant of an enum or a generator, if included */
    @Nullable
    private final Integer variantIndex;

    public MirPlaceTy(@Nonnull Ty ty, @Nullable Integer variantIndex) {
        this.ty = ty;
        this.variantIndex = variantIndex;
    }

    @Nonnull
    public Ty getTy() {
        return ty;
    }

    @Nullable
    public Integer getVariantIndex() {
        return variantIndex;
    }

    // https://github.com/rust-lang/rust/blob/f7b831ac8a897273f78b9f47165cf8e54066ce4b/compiler/rustc_middle/src/mir/tcx.rs#L57
    @Nonnull
    public MirPlaceTy projectionTy(@Nonnull MirProjectionElem<Ty> element) {
        if (variantIndex != null && !(element instanceof MirProjectionElem.Field)) {
            throw new IllegalStateException("cannot use non field projection on downcasted place");
        }
        if (element instanceof MirProjectionElem.Field) {
            MirProjectionElem.Field<Ty> field = (MirProjectionElem.Field<Ty>) element;
            return fromTy(field.getElem());
        } else if (element instanceof MirProjectionElem.Deref) {
            Pair<Ty, ?> deref = TyUtilUtil.builtinDeref(ty, null);
            if (deref == null) {
                throw new IllegalStateException("deref projection of non-dereferenceable ty");
            }
            return fromTy(deref.getFirst());
        } else if (element instanceof MirProjectionElem.Index || element instanceof MirProjectionElem.ConstantIndex) {
            Ty indexedTy = TyUtilUtil.builtinIndex(ty);
            if (indexedTy == null) {
                throw new IllegalStateException("index projection of non-indexable ty");
            }
            return fromTy(indexedTy);
        } else if (element instanceof MirProjectionElem.Downcast) {
            MirProjectionElem.Downcast<Ty> downcast = (MirProjectionElem.Downcast<Ty>) element;
            return new MirPlaceTy(ty, downcast.getVariantIndex());
        } else {
            throw new IllegalStateException("Unknown projection element: " + element);
        }
    }

    @Nonnull
    public static MirPlaceTy fromTy(@Nonnull Ty ty) {
        return new MirPlaceTy(ty, null);
    }
}
