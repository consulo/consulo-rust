/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.types.consts.Const;
import org.rust.lang.core.types.ty.Ty;

public abstract class TypeError {
    private TypeError() {
    }

    public static class TypeMismatch extends TypeError {
        @Nonnull
        private final Ty myTy1;
        @Nonnull
        private final Ty myTy2;

        public TypeMismatch(@Nonnull Ty ty1, @Nonnull Ty ty2) {
            myTy1 = ty1;
            myTy2 = ty2;
        }

        @Nonnull
        public Ty getTy1() {
            return myTy1;
        }

        @Nonnull
        public Ty getTy2() {
            return myTy2;
        }
    }

    public static class ConstMismatch extends TypeError {
        @Nonnull
        private final Const myConst1;
        @Nonnull
        private final Const myConst2;

        public ConstMismatch(@Nonnull Const const1, @Nonnull Const const2) {
            myConst1 = const1;
            myConst2 = const2;
        }

        @Nonnull
        public Const getConst1() {
            return myConst1;
        }

        @Nonnull
        public Const getConst2() {
            return myConst2;
        }
    }
}
