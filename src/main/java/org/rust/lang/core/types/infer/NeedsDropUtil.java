/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsEnumItem;
import org.rust.lang.core.psi.RsStructItem;
import org.rust.lang.core.psi.ext.*;
// import removed - use constant directly
import org.rust.lang.core.resolve.ImplLookup;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.lang.core.types.Substitution;
import org.rust.lang.core.types.ty.*;
import org.rust.lang.utils.evaluation.ThreeValuedLogic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class NeedsDropUtil {
    private NeedsDropUtil() {
    }

    @NotNull
    public static ThreeValuedLogic needsDrop(@NotNull ImplLookup lookup, @NotNull Ty ty, @NotNull RsElement element) {
        int recursionLimit = ImplLookup.DEFAULT_RECURSION_LIMIT;
        return new NeedsDropCheck(lookup, recursionLimit).needsDrop(ty, 0);
    }

    private static class NeedsDropCheck {
        @NotNull
        private final ImplLookup myImplLookup;
        private final int myRecursionLimit;
        @NotNull
        private final Map<Ty, ThreeValuedLogic> myCache = new HashMap<>();

        NeedsDropCheck(@NotNull ImplLookup implLookup, int recursionLimit) {
            myImplLookup = implLookup;
            myRecursionLimit = recursionLimit;
        }

        @NotNull
        ThreeValuedLogic needsDrop(@NotNull Ty ty, int depth) {
            ThreeValuedLogic cached = myCache.get(ty);
            if (cached != null) return cached;
            ThreeValuedLogic result = needsDropRaw(ty, depth);
            myCache.put(ty, result);
            return result;
        }

        @NotNull
        private ThreeValuedLogic needsDropRaw(@NotNull Ty ty, int depth) {
            if (depth > myRecursionLimit) return ThreeValuedLogic.True;
            if (ty instanceof TyUnknown) return ThreeValuedLogic.Unknown;
            if (ty instanceof TyAnon || ty instanceof TyTraitObject) return ThreeValuedLogic.True;
            if (ty instanceof TyPrimitive || ty instanceof TyReference || ty instanceof TyPointer || ty instanceof TyFunctionBase) {
                return ThreeValuedLogic.False;
            }
            if (myImplLookup.isDrop(ty) == ThreeValuedLogic.True) return ThreeValuedLogic.True;
            if (myImplLookup.isCopy(ty) == ThreeValuedLogic.True) return ThreeValuedLogic.False;
            if (ty instanceof TyAdt) return checkAdt((TyAdt) ty, depth);
            if (ty instanceof TyTuple) return checkTuple((TyTuple) ty, depth);
            if (ty instanceof TyArray) return needsDrop(((TyArray) ty).getBase(), depth + 1);
            if (ty instanceof TySlice) return needsDrop(((TySlice) ty).getElementType(), depth + 1);
            return ThreeValuedLogic.Unknown;
        }

        @NotNull
        private ThreeValuedLogic checkAdt(@NotNull TyAdt ty, int depth) {
            Object item = ty.getItem();
            if (item == myImplLookup.getItems().getManuallyDrop()) return ThreeValuedLogic.False;
            if (item instanceof RsStructItem) {
                RsStructItem struct = (RsStructItem) item;
                if (RsStructItemUtil.getKind(struct) == RsStructKind.UNION) return ThreeValuedLogic.False;
                return checkAdtFields(struct, ty.getTypeParameterValues(), depth);
            }
            if (item instanceof RsEnumItem) {
                return checkEnum((RsEnumItem) item, ty.getTypeParameterValues(), depth);
            }
            return ThreeValuedLogic.Unknown;
        }

        @NotNull
        private ThreeValuedLogic checkTuple(@NotNull TyTuple ty, int depth) {
            ThreeValuedLogic result = ThreeValuedLogic.False;
            for (Ty type : ty.getTypes()) {
                result = result.or(needsDrop(type, depth + 1));
                if (result == ThreeValuedLogic.True) return ThreeValuedLogic.True;
            }
            return result;
        }

        @NotNull
        private ThreeValuedLogic checkEnum(@NotNull RsEnumItem enumItem, @NotNull Substitution substitution, int depth) {
            ThreeValuedLogic result = ThreeValuedLogic.False;
            org.rust.lang.core.psi.RsEnumBody body = enumItem.getEnumBody();
            if (body == null) return ThreeValuedLogic.False;
            for (org.rust.lang.core.psi.RsEnumVariant variant : body.getEnumVariantList()) {
                result = result.or(checkAdtFields(variant, substitution, depth));
                if (result == ThreeValuedLogic.True) return ThreeValuedLogic.True;
            }
            return result;
        }

        @NotNull
        private ThreeValuedLogic checkAdtFields(@NotNull RsFieldsOwner fieldsOwner, @NotNull Substitution substitution, int depth) {
            ThreeValuedLogic result = ThreeValuedLogic.False;
            List<?> fields = RsFieldsOwnerExtUtil.getFields(fieldsOwner);
            for (Object field : fields) {
                org.rust.lang.core.psi.RsTypeReference typeRef = null;
                if (field instanceof org.rust.lang.core.psi.RsNamedFieldDecl) {
                    typeRef = ((org.rust.lang.core.psi.RsNamedFieldDecl) field).getTypeReference();
                } else if (field instanceof org.rust.lang.core.psi.RsTupleFieldDecl) {
                    typeRef = ((org.rust.lang.core.psi.RsTupleFieldDecl) field).getTypeReference();
                }
                if (typeRef != null) {
                    Ty fieldTy = FoldUtil.substitute(ExtensionsUtil.getRawType(typeRef), substitution);
                    result = result.or(needsDrop(fieldTy, depth + 1));
                } else {
                    result = result.or(ThreeValuedLogic.Unknown);
                }
                if (result == ThreeValuedLogic.True) return ThreeValuedLogic.True;
            }
            return result;
        }
    }
}
