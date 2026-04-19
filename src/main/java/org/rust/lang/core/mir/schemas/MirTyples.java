/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.mir.MirUtils;
import org.rust.lang.core.types.ty.Mutability;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyInteger;

import java.util.Objects;

/**
 * Contains MirCastTy, MirIntTy, and MirUintTy.
 */
public final class MirTyples {
    private MirTyples() {
    }

    public abstract static class MirCastTy {
        private MirCastTy() {
        }

        public static final class Int extends MirCastTy {
            @NotNull
            private final MirIntTy ty;

            public Int(@NotNull MirIntTy ty) {
                this.ty = ty;
            }

            @NotNull
            public MirIntTy getTy() {
                return ty;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Int anInt = (Int) o;
                return Objects.equals(ty, anInt.ty);
            }

            @Override
            public int hashCode() {
                return Objects.hash(ty);
            }

            @Override
            public String toString() {
                return "Int(ty=" + ty + ")";
            }
        }

        public static final class Float extends MirCastTy {
            public static final Float INSTANCE = new Float();

            private Float() {
            }

            @Override
            public String toString() {
                return "Float";
            }
        }

        public static final class FnPtr extends MirCastTy {
            public static final FnPtr INSTANCE = new FnPtr();

            private FnPtr() {
            }

            @Override
            public String toString() {
                return "FnPtr";
            }
        }

        public static final class Pointer extends MirCastTy {
            @NotNull
            private final Ty ty;
            @NotNull
            private final Mutability mutability;

            public Pointer(@NotNull Ty ty, @NotNull Mutability mutability) {
                this.ty = ty;
                this.mutability = mutability;
            }

            @NotNull
            public Ty getTy() {
                return ty;
            }

            @NotNull
            public Mutability getMutability() {
                return mutability;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                Pointer pointer = (Pointer) o;
                return Objects.equals(ty, pointer.ty) && Objects.equals(mutability, pointer.mutability);
            }

            @Override
            public int hashCode() {
                return Objects.hash(ty, mutability);
            }

            @Override
            public String toString() {
                return "Pointer(ty=" + ty + ", mutability=" + mutability + ")";
            }
        }

        public static final class DynStar extends MirCastTy {
            public static final DynStar INSTANCE = new DynStar();

            private DynStar() {
            }

            @Override
            public String toString() {
                return "DynStar";
            }
        }

        @Nullable
        public static MirCastTy from(@NotNull Ty ty) {
            if (ty instanceof TyInteger && MirUtils.isSigned(ty)) {
                return new Int(MirIntTy.I.INSTANCE);
            } else {
                throw new UnsupportedOperationException("TODO");
            }
        }
    }

    public abstract static class MirIntTy {
        private MirIntTy() {
        }

        public static final class U extends MirIntTy {
            @NotNull
            private final MirUintTy ty;

            public U(@NotNull MirUintTy ty) {
                this.ty = ty;
            }

            @NotNull
            public MirUintTy getTy() {
                return ty;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                U u = (U) o;
                return Objects.equals(ty, u.ty);
            }

            @Override
            public int hashCode() {
                return Objects.hash(ty);
            }

            @Override
            public String toString() {
                return "U(ty=" + ty + ")";
            }
        }

        public static final class I extends MirIntTy {
            public static final I INSTANCE = new I();

            private I() {
            }

            @Override
            public String toString() {
                return "I";
            }
        }

        public static final class CEnum extends MirIntTy {
            public static final CEnum INSTANCE = new CEnum();

            private CEnum() {
            }

            @Override
            public String toString() {
                return "CEnum";
            }
        }

        public static final class Bool extends MirIntTy {
            public static final Bool INSTANCE = new Bool();

            private Bool() {
            }

            @Override
            public String toString() {
                return "Bool";
            }
        }

        public static final class Char extends MirIntTy {
            public static final Char INSTANCE = new Char();

            private Char() {
            }

            @Override
            public String toString() {
                return "Char";
            }
        }
    }

    // This is the only class that is in the type system module in compiler, but we don't have TyUInteger or smth
    public enum MirUintTy {
        Usize,
        U8,
        U16,
        U32,
        U64,
        U128,
    }
}
