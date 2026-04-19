/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsPath;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.resolve.RsPathResolveResult;

import java.util.List;

public abstract class TyPrimitive extends Ty {
    protected TyPrimitive() {
        super();
    }

    protected TyPrimitive(int flags) {
        super(flags);
    }

    @NotNull
    public abstract String getName();

    @Nullable
    public static TyPrimitive fromPath(@NotNull RsPath path) {
        return fromPath(path, null);
    }

    @Nullable
    public static TyPrimitive fromPath(@NotNull RsPath path, @Nullable List<RsPathResolveResult<RsElement>> givenResolveResult) {
        String name = path.getReferenceName();
        if (name == null) return null;
        if (path.getPath() != null || path.getTypeQual() != null) return null;

        TyPrimitive ty = fromName(name);
        if (ty != null) {
            // Check if it's actually a primitive or a user-defined type with the same name
            List<RsPathResolveResult<RsElement>> resolveResult = givenResolveResult != null
                ? givenResolveResult
                : null; // Would normally resolve here
            // Simplified: just return based on name
        }
        return ty;
    }

    @Nullable
    private static TyPrimitive fromName(@NotNull String name) {
        switch (name) {
            case "bool": return TyBool.INSTANCE;
            case "char": return TyChar.INSTANCE;
            case "str": return TyStr.INSTANCE;
            case "i8": return TyInteger.I8.INSTANCE;
            case "i16": return TyInteger.I16.INSTANCE;
            case "i32": return TyInteger.I32.INSTANCE;
            case "i64": return TyInteger.I64.INSTANCE;
            case "i128": return TyInteger.I128.INSTANCE;
            case "isize": return TyInteger.ISize.INSTANCE;
            case "u8": return TyInteger.U8.INSTANCE;
            case "u16": return TyInteger.U16.INSTANCE;
            case "u32": return TyInteger.U32.INSTANCE;
            case "u64": return TyInteger.U64.INSTANCE;
            case "u128": return TyInteger.U128.INSTANCE;
            case "usize": return TyInteger.USize.INSTANCE;
            case "f32": return TyFloat.F32.INSTANCE;
            case "f64": return TyFloat.F64.INSTANCE;
            default: return null;
        }
    }
}
