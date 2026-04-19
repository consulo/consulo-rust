/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty;

import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsStructItem;
import org.rust.lang.core.psi.RsTypeAlias;
import org.rust.lang.core.psi.ext.RsFieldsOwnerExtUtil;
import org.rust.lang.core.resolve.ImplLookup;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.types.BoundElement;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.lang.core.types.Substitution;
import org.rust.lang.core.types.infer.FoldUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class TyUtil {
    private TyUtil() {
    }

    @Nullable
    public static TyTypeParameter getTypeParameter(@NotNull Ty ty, @NotNull String name) {
        return ty.getTypeParameterValues().typeParameterByName(name);
    }

    public static boolean isSelf(@NotNull Ty ty) {
        return ty instanceof TyTypeParameter && ((TyTypeParameter) ty).getParameter() instanceof TyTypeParameter.Self;
    }

    @NotNull
    public static TypeIterator walk(@NotNull Ty ty) {
        return new TypeIterator(ty);
    }

    @Nullable
    public static Pair<Ty, Mutability> builtinDeref(@NotNull Ty ty, @Nullable KnownItems items, boolean explicit) {
        if (ty instanceof TyAdt) {
            TyAdt adt = (TyAdt) ty;
            KnownItems knownItems = items != null ? items : KnownItems.getKnownItems(adt.getItem());
            if (adt.getItem() == knownItems.getBox()) {
                List<Ty> typeArgs = adt.getTypeArguments();
                Ty first = typeArgs.isEmpty() ? TyUnknown.INSTANCE : typeArgs.get(0);
                return new Pair<>(first, Mutability.IMMUTABLE);
            }
        }
        if (ty instanceof TyReference) {
            return new Pair<>(((TyReference) ty).getReferenced(), ((TyReference) ty).getMutability());
        }
        if (ty instanceof TyPointer && explicit) {
            return new Pair<>(((TyPointer) ty).getReferenced(), ((TyPointer) ty).getMutability());
        }
        return null;
    }

    @Nullable
    public static Pair<Ty, Mutability> builtinDeref(@NotNull Ty ty, @Nullable KnownItems items) {
        return builtinDeref(ty, items, true);
    }

    @Nullable
    public static Ty builtinIndex(@NotNull Ty ty) {
        if (ty instanceof TyArray) return ((TyArray) ty).getBase();
        if (ty instanceof TySlice) return ((TySlice) ty).getElementType();
        return null;
    }

    @NotNull
    public static Ty stripReferences(@NotNull Ty ty) {
        Ty current = ty;
        while (current instanceof TyReference) {
            current = ((TyReference) current).getReferenced();
        }
        return current;
    }

    @Nullable
    public static Ty structTail(@NotNull Ty ty) {
        Set<Ty> ancestors = new HashSet<>();
        ancestors.add(ty);
        return structTailInner(ty, ancestors);
    }

    @Nullable
    private static Ty structTailInner(@NotNull Ty ty, @NotNull Set<Ty> ancestors) {
        if (ty instanceof TyAdt) {
            TyAdt adt = (TyAdt) ty;
            if (!(adt.getItem() instanceof RsStructItem)) return ty;
            RsStructItem struct = (RsStructItem) adt.getItem();
            List<?> fields = RsFieldsOwnerExtUtil.getFields(struct);
            if (fields.isEmpty()) return null;
            Object lastField = fields.get(fields.size() - 1);
            org.rust.lang.core.psi.RsTypeReference typeRef =
                ((org.rust.lang.core.psi.ext.RsFieldDecl) lastField).getTypeReference();
            if (typeRef == null) return null;
            Ty fieldTy = FoldUtil.substitute(ExtensionsUtil.getRawType(typeRef), adt.getTypeParameterValues());
            if (!ancestors.add(fieldTy)) return null;
            return structTailInner(fieldTy, ancestors);
        }
        if (ty instanceof TyTuple) {
            TyTuple tuple = (TyTuple) ty;
            List<Ty> types = tuple.getTypes();
            return structTailInner(types.get(types.size() - 1), ancestors);
        }
        return ty;
    }

    public static boolean isMovesByDefault(@NotNull Ty ty, @NotNull ImplLookup lookup) {
        if (ty instanceof TyUnknown || ty instanceof TyReference || ty instanceof TyPointer) return false;
        if (ty instanceof TyTuple) {
            for (Ty t : ((TyTuple) ty).getTypes()) {
                if (isMovesByDefault(t, lookup)) return true;
            }
            return false;
        }
        if (ty instanceof TyArray) return isMovesByDefault(((TyArray) ty).getBase(), lookup);
        if (ty instanceof TySlice) return isMovesByDefault(((TySlice) ty).getElementType(), lookup);
        if (ty instanceof TyTypeParameter) {
            TyTypeParameter tp = (TyTypeParameter) ty;
            return !(tp.getParameter() instanceof TyTypeParameter.Self) && lookup.isCopy(ty).isFalse();
        }
        return lookup.isCopy(ty).isFalse();
    }

    public static boolean isBox(@NotNull Ty ty) {
        if (!(ty instanceof TyAdt)) return false;
        TyAdt adt = (TyAdt) ty;
        return adt.getItem() == KnownItems.getKnownItems(adt.getItem()).getBox();
    }

    public static boolean isIntegral(@NotNull Ty ty) {
        return ty instanceof TyInteger || ty instanceof TyInfer.IntVar;
    }

    public static boolean isFloat(@NotNull Ty ty) {
        return ty instanceof TyFloat || ty instanceof TyInfer.FloatVar;
    }

    public static boolean isScalar(@NotNull Ty ty) {
        return isIntegral(ty)
            || isFloat(ty)
            || ty instanceof TyBool
            || ty instanceof TyChar
            || ty instanceof TyFunctionDef
            || ty instanceof TyFunctionPointer
            || ty instanceof TyPointer;
    }
}
