/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public abstract class TyFloat extends TyPrimitive {
    public static final TyFloat DEFAULT = F64.INSTANCE;
    public static final Set<String> NAMES = new HashSet<>(Arrays.asList("f32", "f64"));

    protected TyFloat() {
        super();
    }

    public static final class F32 extends TyFloat {
        public static final F32 INSTANCE = new F32();
        @NotNull @Override public String getName() { return "f32"; }
    }
    public static final class F64 extends TyFloat {
        public static final F64 INSTANCE = new F64();
        @NotNull @Override public String getName() { return "f64"; }
    }
}
