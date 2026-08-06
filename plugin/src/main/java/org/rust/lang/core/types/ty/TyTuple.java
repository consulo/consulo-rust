/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.types.KindUtil;
import org.rust.lang.core.types.infer.TypeFolder;
import org.rust.lang.core.types.infer.TypeVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TyTuple extends Ty {
    @Nonnull
    private final List<Ty> myTypes;

    public TyTuple(@Nonnull List<Ty> types) {
        super(KindUtil.mergeFlags(types));
        myTypes = types;
    }

    @Nonnull
    public List<Ty> getTypes() {
        return myTypes;
    }

    @Override
    @Nonnull
    public Ty superFoldWith(@Nonnull TypeFolder folder) {
        List<Ty> newTypes = new ArrayList<>();
        for (Ty ty : myTypes) {
            newTypes.add(ty.foldWith(folder));
        }
        return new TyTuple(newTypes);
    }

    @Override
    public boolean superVisitWith(@Nonnull TypeVisitor visitor) {
        for (Ty ty : myTypes) {
            if (ty.visitWith(visitor)) return true;
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TyTuple tyTuple = (TyTuple) o;
        return Objects.equals(myTypes, tyTuple.myTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myTypes);
    }
}
