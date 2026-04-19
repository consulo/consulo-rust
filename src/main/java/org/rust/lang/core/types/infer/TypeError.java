/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.types.consts.Const;
import org.rust.lang.core.types.ty.Ty;

public abstract class TypeError {
    private TypeError() {
    }

    public static class TypeMismatch extends TypeError {
        @NotNull
        private final Ty myTy1;
        @NotNull
        private final Ty myTy2;

        public TypeMismatch(@NotNull Ty ty1, @NotNull Ty ty2) {
            myTy1 = ty1;
            myTy2 = ty2;
        }

        @NotNull
        public Ty getTy1() {
            return myTy1;
        }

        @NotNull
        public Ty getTy2() {
            return myTy2;
        }
    }

    public static class ConstMismatch extends TypeError {
        @NotNull
        private final Const myConst1;
        @NotNull
        private final Const myConst2;

        public ConstMismatch(@NotNull Const const1, @NotNull Const const2) {
            myConst1 = const1;
            myConst2 = const2;
        }

        @NotNull
        public Const getConst1() {
            return myConst1;
        }

        @NotNull
        public Const getConst2() {
            return myConst2;
        }
    }
}
