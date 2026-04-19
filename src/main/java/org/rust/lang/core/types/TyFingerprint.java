/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsRefLikeTypeUtil;
import org.rust.lang.core.psi.ext.RsArrayTypeExtUtil;
import org.rust.lang.core.psi.ext.RsTraitTypeExtUtil;
import org.rust.lang.core.psi.ext.RsTypeReferenceExtUtil;
import org.rust.lang.core.types.ty.*;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;

/**
 * A type fingerprint used for indexing. It should satisfy two properties:
 *
 *  - ty1 == ty2 => fingerprint(ty1) == fingerprint(ty2).
 *  - fingerprint can be computed without name resolution.
 */
public final class TyFingerprint {
    @NotNull
    private final String myName;

    public static final TyFingerprint TYPE_PARAMETER_OR_MACRO_FINGERPRINT = new TyFingerprint("#T");
    private static final TyFingerprint ANY_INTEGER_FINGERPRINT = new TyFingerprint("{integer}");
    private static final TyFingerprint ANY_FLOAT_FINGERPRINT = new TyFingerprint("{float}");

    public TyFingerprint(@NotNull String name) {
        myName = name;
    }

    @NotNull
    public String getName() {
        return myName;
    }

    @NotNull
    public static List<TyFingerprint> create(@NotNull RsTypeReference ref, @NotNull List<String> typeParameters) {
        RsTypeReference type = RsTypeReferenceExtUtil.skipParens(ref);
        if (type instanceof RsTupleType) {
            return Collections.singletonList(new TyFingerprint("(tuple)"));
        } else if (type instanceof RsUnitType) {
            return Collections.singletonList(new TyFingerprint("()"));
        } else if (type instanceof RsNeverType) {
            return Collections.singletonList(new TyFingerprint("!"));
        } else if (type instanceof RsInferType) {
            return Collections.emptyList();
        } else if (type instanceof RsPathType) {
            String name = ((RsPathType) type).getPath().getReferenceName();
            if (name == null) return Collections.emptyList();
            if (typeParameters.contains(name)) return Collections.singletonList(TYPE_PARAMETER_OR_MACRO_FINGERPRINT);
            if (TyInteger.NAMES.contains(name)) return Arrays.asList(new TyFingerprint(name), ANY_INTEGER_FINGERPRINT);
            if (TyFloat.NAMES.contains(name)) return Arrays.asList(new TyFingerprint(name), ANY_FLOAT_FINGERPRINT);
            return Collections.singletonList(new TyFingerprint(name));
        } else if (type instanceof RsRefLikeType) {
            if (RsRefLikeTypeUtil.isPointer((RsRefLikeType) type)) {
                return Collections.singletonList(new TyFingerprint("*T"));
            } else {
                RsTypeReference inner = ((RsRefLikeType) type).getTypeReference();
                return inner != null ? create(inner, typeParameters) : Collections.emptyList();
            }
        } else if (type instanceof RsArrayType) {
            if (RsArrayTypeExtUtil.isSlice((RsArrayType) type)) {
                return Collections.singletonList(new TyFingerprint("[T]"));
            } else {
                return Collections.singletonList(new TyFingerprint("[T;]"));
            }
        } else if (type instanceof RsFnPointerType) {
            return Collections.singletonList(new TyFingerprint("fn()"));
        } else if (type instanceof RsTraitType) {
            if (!RsTraitTypeExtUtil.isImpl((RsTraitType) type)) {
                return Collections.singletonList(new TyFingerprint("dyn T"));
            }
            return Collections.emptyList();
        } else if (type instanceof RsMacroType) {
            return Collections.singletonList(TYPE_PARAMETER_OR_MACRO_FINGERPRINT);
        }
        return Collections.emptyList();
    }

    @Nullable
    public static TyFingerprint create(@NotNull Ty type) {
        if (type instanceof TyAdt) {
            String name = ((TyAdt) type).getItem().getName();
            return name != null ? new TyFingerprint(name) : null;
        } else if (type instanceof TySlice) {
            return new TyFingerprint("[T]");
        } else if (type instanceof TyArray) {
            return new TyFingerprint("[T;]");
        } else if (type instanceof TyPointer) {
            return new TyFingerprint("*T");
        } else if (type instanceof TyReference) {
            return create(((TyReference) type).getReferenced());
        } else if (type instanceof TyTuple) {
            return new TyFingerprint("(tuple)");
        } else if (type instanceof TyPrimitive) {
            return new TyFingerprint(type.toString());
        } else if (type instanceof TyFunctionBase) {
            return new TyFingerprint("fn()");
        } else if (type instanceof TyTraitObject) {
            return new TyFingerprint("dyn T");
        } else if (type instanceof TyInfer.IntVar) {
            return ANY_INTEGER_FINGERPRINT;
        } else if (type instanceof TyInfer.FloatVar) {
            return ANY_FLOAT_FINGERPRINT;
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TyFingerprint that = (TyFingerprint) o;
        return myName.equals(that.myName);
    }

    @Override
    public int hashCode() {
        return myName.hashCode();
    }

    @Override
    public String toString() {
        return "TyFingerprint(" + myName + ")";
    }

    public static final com.intellij.util.io.KeyDescriptor<TyFingerprint> KEY_DESCRIPTOR =
        new com.intellij.util.io.KeyDescriptor<TyFingerprint>() {
            @Override
            public void save(@NotNull DataOutput out, TyFingerprint value) throws IOException {
                out.writeUTF(value.myName);
            }

            @Override
            public TyFingerprint read(@NotNull DataInput in) throws IOException {
                return new TyFingerprint(in.readUTF());
            }

            @Override
            public int getHashCode(TyFingerprint value) {
                return value.hashCode();
            }

            @Override
            public boolean isEqual(TyFingerprint lhs, TyFingerprint rhs) {
                return lhs.equals(rhs);
            }
        };
}
