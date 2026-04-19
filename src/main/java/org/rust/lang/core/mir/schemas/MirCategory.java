/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.thir.ThirExpr;

import java.util.Objects;

public abstract class MirCategory {
    private MirCategory() {
    }

    public static final class Place extends MirCategory {
        public static final Place INSTANCE = new Place();

        private Place() {
        }

        @Override
        public String toString() {
            return "Place";
        }
    }

    public static final class Constant extends MirCategory {
        public static final Constant INSTANCE = new Constant();

        private Constant() {
        }

        @Override
        public String toString() {
            return "Constant";
        }
    }

    public static final class Rvalue extends MirCategory {
        @NotNull
        private final MirRvalueFunc value;

        public Rvalue(@NotNull MirRvalueFunc value) {
            this.value = value;
        }

        @NotNull
        public MirRvalueFunc getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Rvalue rvalue = (Rvalue) o;
            return value == rvalue.value;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }

        @Override
        public String toString() {
            return "Rvalue(value=" + value + ")";
        }
    }

    // https://github.com/rust-lang/rust/blob/f7b831ac8a897273f78b9f47165cf8e54066ce4b/compiler/rustc_mir_build/src/build/expr/category.rs#L34
    @Nullable
    public static MirCategory of(@NotNull ThirExpr element) {
        if (element instanceof ThirExpr.Deref
            || element instanceof ThirExpr.Field
            || element instanceof ThirExpr.Index
            || element instanceof ThirExpr.UpvarRef
            || element instanceof ThirExpr.VarRef
            || element instanceof ThirExpr.PlaceTypeAscription
            || element instanceof ThirExpr.ValueTypeAscription) {
            return Place.INSTANCE;
        } else if (element instanceof ThirExpr.ConstBlock
            || element instanceof ThirExpr.Literal
            || element instanceof ThirExpr.NonHirLiteral
            || element instanceof ThirExpr.ZstLiteral
            || element instanceof ThirExpr.ConstParam
            || element instanceof ThirExpr.StaticRef
            || element instanceof ThirExpr.NamedConst) {
            return Constant.INSTANCE;
        } else if (element instanceof ThirExpr.Array
            || element instanceof ThirExpr.Tuple
            || element instanceof ThirExpr.Closure
            || element instanceof ThirExpr.Unary
            || element instanceof ThirExpr.Binary
            || element instanceof ThirExpr.BoxExpr
            || element instanceof ThirExpr.Cast
            || element instanceof ThirExpr.Pointer
            || element instanceof ThirExpr.Repeat
            || element instanceof ThirExpr.Assign
            || element instanceof ThirExpr.AssignOp
            || element instanceof ThirExpr.ThreadLocalRef
            || element instanceof ThirExpr.OffsetOf) {
            return new Rvalue(MirRvalueFunc.AS_RVALUE);
        } else if (element instanceof ThirExpr.Logical
            || element instanceof ThirExpr.Match
            || element instanceof ThirExpr.If
            || element instanceof ThirExpr.Let
            || element instanceof ThirExpr.NeverToAny
            || element instanceof ThirExpr.Use
            || element instanceof ThirExpr.Adt
            || element instanceof ThirExpr.Borrow
            || element instanceof ThirExpr.AddressOf
            || element instanceof ThirExpr.Yield
            || element instanceof ThirExpr.Call
            || element instanceof ThirExpr.InlineAsm
            || element instanceof ThirExpr.Loop
            || element instanceof ThirExpr.Block
            || element instanceof ThirExpr.Break
            || element instanceof ThirExpr.Continue
            || element instanceof ThirExpr.Return) {
            return new Rvalue(MirRvalueFunc.INTO);
        } else if (element instanceof ThirExpr.ScopeExpr) {
            return null;
        } else {
            throw new IllegalStateException("Unknown ThirExpr type: " + element.getClass());
        }
    }
}
