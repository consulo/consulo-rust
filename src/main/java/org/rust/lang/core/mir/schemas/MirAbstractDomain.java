/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import org.jetbrains.annotations.NotNull;

public final class MirAbstractDomain {

    private MirAbstractDomain() {
    }

    public static final MirAbstractType ABSTRACT_TYPE = MirAbstractType.INSTANCE;

    /**
     * Lifts a PlaceElem (MirProjectionElem of Ty) to MirProjectionElem of MirAbstractType.
     */
    @NotNull
    public static MirProjectionElem<MirAbstractType> lift(@NotNull MirProjectionElem<?> elem) {
        if (elem instanceof MirProjectionElem.Deref) {
            return MirProjectionElem.Deref.INSTANCE;
        } else if (elem instanceof MirProjectionElem.Field) {
            MirProjectionElem.Field<?> field = (MirProjectionElem.Field<?>) elem;
            return new MirProjectionElem.Field<>(field.getFieldIndex(), MirAbstractType.INSTANCE);
        } else if (elem instanceof MirProjectionElem.Index) {
            MirProjectionElem.Index<?> index = (MirProjectionElem.Index<?>) elem;
            return new MirProjectionElem.Index<>(index.getIndex());
        } else if (elem instanceof MirProjectionElem.ConstantIndex) {
            MirProjectionElem.ConstantIndex<?> ci = (MirProjectionElem.ConstantIndex<?>) elem;
            return new MirProjectionElem.ConstantIndex<>(ci.getOffset(), ci.getMinLength(), ci.isFromEnd());
        } else if (elem instanceof MirProjectionElem.Downcast) {
            MirProjectionElem.Downcast<?> dc = (MirProjectionElem.Downcast<?>) elem;
            return new MirProjectionElem.Downcast<>(dc.getName(), dc.getVariantIndex());
        } else {
            throw new IllegalStateException("Unknown projection element: " + elem);
        }
    }
}
