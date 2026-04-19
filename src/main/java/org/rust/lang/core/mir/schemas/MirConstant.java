/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsConstant;
import org.rust.lang.core.types.ty.Ty;

public abstract class MirConstant {
    @NotNull
    private final MirSpan span;

    protected MirConstant(@NotNull MirSpan span) {
        this.span = span;
    }

    @NotNull
    public MirSpan getSpan() {
        return span;
    }

    public static final class Value extends MirConstant {
        @NotNull
        private final MirConstValue constValue;
        @NotNull
        private final Ty ty;

        public Value(@NotNull MirConstValue constValue, @NotNull Ty ty, @NotNull MirSpan span) {
            super(span);
            this.constValue = constValue;
            this.ty = ty;
        }

        @NotNull
        public MirConstValue getConstValue() {
            return constValue;
        }

        @NotNull
        public Ty getTy() {
            return ty;
        }

        @Override
        public String toString() {
            return "Value(constValue=" + constValue + ", ty=" + ty + ")";
        }
    }

    public static final class Unevaluated extends MirConstant {
        @NotNull
        private final RsConstant def;
        @NotNull
        private final Ty ty;

        public Unevaluated(@NotNull RsConstant def, @NotNull Ty ty, @NotNull MirSpan span) {
            super(span);
            this.def = def;
            this.ty = ty;
        }

        @NotNull
        public RsConstant getDef() {
            return def;
        }

        @NotNull
        public Ty getTy() {
            return ty;
        }
    }

    @NotNull
    public static MirConstant zeroSized(@NotNull Ty ty, @NotNull MirSpan span) {
        return new Value(MirConstValue.ZeroSized.INSTANCE, ty, span);
    }
}
