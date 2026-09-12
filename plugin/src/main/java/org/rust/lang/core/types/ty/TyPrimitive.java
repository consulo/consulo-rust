/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import consulo.language.psi.PsiElement;
import org.rust.lang.core.psi.RsPath;
import org.rust.lang.core.psi.RsPathType;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.RsPathUtil;
import org.rust.lang.core.resolve.RsPathResolveResult;
import org.rust.lang.core.resolve.ref.RsPathReference;

import java.util.List;

public abstract class TyPrimitive extends Ty {
    protected TyPrimitive() {
        super();
    }

    protected TyPrimitive(int flags) {
        super(flags);
    }

    @Nonnull
    public abstract String getName();

    @Nullable
    public static TyPrimitive fromPath(@Nonnull RsPath path) {
        return fromPath(path, true, null);
    }

    @Nullable
    public static TyPrimitive fromPath(@Nonnull RsPath path, @Nullable List<RsPathResolveResult<RsElement>> givenResolveResult) {
        return fromPath(path, true, givenResolveResult);
    }

    /**
     * The primitive type a path denotes, or {@code null} if it denotes something else.
     *
     * @param checkResolve        when {@code true}, a path that resolves to a user-declared item of the same
     *                            name (e.g. {@code struct u8;}) denotes that item, not the primitive
     * @param givenResolveResult  an already computed resolve result for {@code path}, to avoid resolving again
     */
    @Nullable
    public static TyPrimitive fromPath(
        @Nonnull RsPath path,
        boolean checkResolve,
        @Nullable List<RsPathResolveResult<RsElement>> givenResolveResult
    ) {
        String name = path.getReferenceName();
        if (name == null) return null;

        TyPrimitive result = fromName(name);
        if (result == null) return null;

        if (RsPathUtil.getHasColonColon(path) || path.getTypeQual() != null) return null;
        PsiElement parent = path.getParent();
        if (!(parent instanceof RsPathType) && !(parent instanceof RsPath)) return null;

        // struct u8;
        // let a: u8; // this is a struct "u8", not a primitive type "u8"
        if (checkResolve) {
            List<RsPathResolveResult<RsElement>> resolvedTo = givenResolveResult;
            if (resolvedTo == null) {
                RsPathReference reference = path.getReference();
                if (reference == null) return null;
                resolvedTo = reference.rawMultiResolve();
            }
            if (parent instanceof RsPathType) {
                for (RsPathResolveResult<RsElement> resolveResult : resolvedTo) {
                    if (!(resolveResult.getElement() instanceof RsMod)) return null;
                }
            }
            if (parent instanceof RsPath && !resolvedTo.isEmpty()) return null;
        }

        return result;
    }

    @Nullable
    private static TyPrimitive fromName(@Nonnull String name) {
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
