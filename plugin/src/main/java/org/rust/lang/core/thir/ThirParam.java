/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.thir;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.mir.schemas.MirSpan;
import org.rust.lang.core.types.ty.Ty;

import java.util.Objects;

public class ThirParam {
    @Nullable
    public final ThirPat pat;
    @Nonnull
    public final Ty ty;
    @Nullable
    public final MirSpan tySpan;
    @Nullable
    public final ImplicitSelfKind selfKind;

    public ThirParam(
        @Nullable ThirPat pat,
        @Nonnull Ty ty,
        @Nullable MirSpan tySpan,
        @Nullable ImplicitSelfKind selfKind
    ) {
        this.pat = pat;
        this.ty = ty;
        this.tySpan = tySpan;
        this.selfKind = selfKind;
    }

    @Nullable
    public ThirPat getPat() {
        return pat;
    }

    @Nonnull
    public Ty getTy() {
        return ty;
    }

    @Nullable
    public MirSpan getTySpan() {
        return tySpan;
    }

    @Nullable
    public ImplicitSelfKind getSelfKind() {
        return selfKind;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ThirParam)) return false;
        ThirParam that = (ThirParam) o;
        return Objects.equals(pat, that.pat) &&
            ty.equals(that.ty) &&
            Objects.equals(tySpan, that.tySpan) &&
            selfKind == that.selfKind;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pat, ty, tySpan, selfKind);
    }
}
