/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsConstant;
import org.rust.lang.core.types.ty.Ty;

public abstract class MirConstant {
    @Nonnull
    private final MirSpan span;

    protected MirConstant(@Nonnull MirSpan span) {
        this.span = span;
    }

    @Nonnull
    public MirSpan getSpan() {
        return span;
    }

    public static final class Value extends MirConstant {
        @Nonnull
        private final MirConstValue constValue;
        @Nonnull
        private final Ty ty;

        public Value(@Nonnull MirConstValue constValue, @Nonnull Ty ty, @Nonnull MirSpan span) {
            super(span);
            this.constValue = constValue;
            this.ty = ty;
        }

        @Nonnull
        public MirConstValue getConstValue() {
            return constValue;
        }

        @Nonnull
        public Ty getTy() {
            return ty;
        }

        @Override
        public String toString() {
            return "Value(constValue=" + constValue + ", ty=" + ty + ")";
        }
    }

    public static final class Unevaluated extends MirConstant {
        @Nonnull
        private final RsConstant def;
        @Nonnull
        private final Ty ty;

        public Unevaluated(@Nonnull RsConstant def, @Nonnull Ty ty, @Nonnull MirSpan span) {
            super(span);
            this.def = def;
            this.ty = ty;
        }

        @Nonnull
        public RsConstant getDef() {
            return def;
        }

        @Nonnull
        public Ty getTy() {
            return ty;
        }
    }

    @Nonnull
    public static MirConstant zeroSized(@Nonnull Ty ty, @Nonnull MirSpan span) {
        return new Value(MirConstValue.ZeroSized.INSTANCE, ty, span);
    }
}
