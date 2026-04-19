/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public abstract class TyInteger extends TyPrimitive {
    public static final TyInteger DEFAULT = I32.INSTANCE;
    public static final Set<String> NAMES = new HashSet<>(Arrays.asList(
        "i8", "i16", "i32", "i64", "i128", "isize",
        "u8", "u16", "u32", "u64", "u128", "usize"
    ));

    protected TyInteger() {
        super();
    }

    public static final class I8 extends TyInteger {
        public static final I8 INSTANCE = new I8();
        @NotNull @Override public String getName() { return "i8"; }
    }
    public static final class I16 extends TyInteger {
        public static final I16 INSTANCE = new I16();
        @NotNull @Override public String getName() { return "i16"; }
    }
    public static final class I32 extends TyInteger {
        public static final I32 INSTANCE = new I32();
        @NotNull @Override public String getName() { return "i32"; }
    }
    public static final class I64 extends TyInteger {
        public static final I64 INSTANCE = new I64();
        @NotNull @Override public String getName() { return "i64"; }
    }
    public static final class I128 extends TyInteger {
        public static final I128 INSTANCE = new I128();
        @NotNull @Override public String getName() { return "i128"; }
    }
    public static final class ISize extends TyInteger {
        public static final ISize INSTANCE = new ISize();
        @NotNull @Override public String getName() { return "isize"; }
    }
    public static final class U8 extends TyInteger {
        public static final U8 INSTANCE = new U8();
        @NotNull @Override public String getName() { return "u8"; }
    }
    public static final class U16 extends TyInteger {
        public static final U16 INSTANCE = new U16();
        @NotNull @Override public String getName() { return "u16"; }
    }
    public static final class U32 extends TyInteger {
        public static final U32 INSTANCE = new U32();
        @NotNull @Override public String getName() { return "u32"; }
    }
    public static final class U64 extends TyInteger {
        public static final U64 INSTANCE = new U64();
        @NotNull @Override public String getName() { return "u64"; }
    }
    public static final class U128 extends TyInteger {
        public static final U128 INSTANCE = new U128();
        @NotNull @Override public String getName() { return "u128"; }
    }
    public static final class USize extends TyInteger {
        public static final USize INSTANCE = new USize();
        @NotNull @Override public String getName() { return "usize"; }
    }
}
