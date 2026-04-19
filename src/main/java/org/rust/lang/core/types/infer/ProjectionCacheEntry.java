/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.types.ty.Ty;

public abstract class ProjectionCacheEntry {
    private ProjectionCacheEntry() {}

    public static final class InProgress extends ProjectionCacheEntry {
        public static final InProgress INSTANCE = new InProgress();
    }

    public static final class Ambiguous extends ProjectionCacheEntry {
        public static final Ambiguous INSTANCE = new Ambiguous();
    }

    public static final class Error extends ProjectionCacheEntry {
        public static final Error INSTANCE = new Error();
    }

    public static final class NormalizedTy extends ProjectionCacheEntry {
        @NotNull
        private final TyWithObligations<Ty> myTy;

        public NormalizedTy(@NotNull TyWithObligations<Ty> ty) {
            myTy = ty;
        }

        @NotNull
        public TyWithObligations<Ty> getTy() {
            return myTy;
        }
    }
}
