/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyUnknown;

import java.util.ArrayList;
import java.util.List;
import org.rust.lang.core.types.RsTypesUtil;

/**
 * An element that can be called using RsCallExpr.
 */
public interface RsCallable {
    @Nullable
    RsSelfParameter getSelfParameter();

    @NotNull
    List<Ty> getParameterTypes();

    @NotNull
    Ty getRawReturnType();

    boolean isAsync();

    @NotNull
    KnownItems getKnownItems();

    boolean isActuallyUnsafe();

    @NotNull
    RsAbstractableOwner getOwner();

    boolean isVariadic();

    @Nullable
    String getName();

    boolean isIntrinsic();

    @NotNull
    QueryAttributes<RsMetaItem> getQueryAttributes();

    class Function implements RsCallable {
        @NotNull
        private final RsFunction myFn;

        public Function(@NotNull RsFunction fn) {
            myFn = fn;
        }

        @NotNull
        public RsFunction getFn() {
            return myFn;
        }

        @Override
        @Nullable
        public String getName() {
            return myFn.getName();
        }

        @Override
        @Nullable
        public RsSelfParameter getSelfParameter() {
            return myFn.getSelfParameter();
        }

        @Override
        @NotNull
        public List<Ty> getParameterTypes() {
            List<Ty> result = new ArrayList<>();
            for (RsValueParameter param : myFn.getValueParameters()) {
                RsTypeReference typeRef = param.getTypeReference();
                result.add(typeRef != null ? RsTypesUtil.getRawType(typeRef) : TyUnknown.INSTANCE);
            }
            return result;
        }

        @Override
        @NotNull
        public Ty getRawReturnType() {
            return RsFunctionUtil.getRawReturnType(myFn);
        }

        @Override
        public boolean isAsync() {
            return RsFunctionUtil.isAsync(myFn);
        }

        @Override
        @NotNull
        public KnownItems getKnownItems() {
            return KnownItems.getKnownItems(myFn);
        }

        @Override
        public boolean isActuallyUnsafe() {
            return RsFunctionUtil.isActuallyUnsafe(myFn);
        }

        @Override
        @NotNull
        public RsAbstractableOwner getOwner() {
            return RsAbstractableOwnerUtil.getOwner(myFn);
        }

        @Override
        public boolean isVariadic() {
            return RsFunctionUtil.isVariadic(myFn);
        }

        @Override
        public boolean isIntrinsic() {
            return RsFunctionUtil.isIntrinsic(myFn);
        }

        @Override
        @NotNull
        public QueryAttributes<RsMetaItem> getQueryAttributes() {
            return RsAttrProcMacroOwnerExtUtil.getQueryAttributes(myFn);
        }
    }

    class EnumVariant implements RsCallable {
        @NotNull
        private final RsEnumVariant myEnumVariant;

        public EnumVariant(@NotNull RsEnumVariant enumVariant) {
            myEnumVariant = enumVariant;
        }

        @NotNull
        public RsEnumVariant getEnumVariant() {
            return myEnumVariant;
        }

        @Override
        @Nullable
        public String getName() {
            return myEnumVariant.getName();
        }

        @Override
        @Nullable
        public RsSelfParameter getSelfParameter() {
            return null;
        }

        @Override
        @NotNull
        public List<Ty> getParameterTypes() {
            return RsFieldsOwnerExtUtil.getFieldTypes(myEnumVariant);
        }

        @Override
        @NotNull
        public Ty getRawReturnType() {
            return RsEnumVariantUtil.getParentEnum(myEnumVariant).getDeclaredType();
        }

        @Override
        public boolean isAsync() {
            return false;
        }

        @Override
        @NotNull
        public KnownItems getKnownItems() {
            return KnownItems.getKnownItems(myEnumVariant);
        }

        @Override
        public boolean isActuallyUnsafe() {
            return false;
        }

        @Override
        @NotNull
        public RsAbstractableOwner getOwner() {
            return RsAbstractableOwner.Free;
        }

        @Override
        public boolean isVariadic() {
            return false;
        }

        @Override
        public boolean isIntrinsic() {
            return false;
        }

        @Override
        @NotNull
        public QueryAttributes<RsMetaItem> getQueryAttributes() {
            return RsAttrProcMacroOwnerExtUtil.getQueryAttributes(myEnumVariant);
        }
    }

    class StructItem implements RsCallable {
        @NotNull
        private final RsStructItem myStructItem;

        public StructItem(@NotNull RsStructItem structItem) {
            myStructItem = structItem;
        }

        @NotNull
        public RsStructItem getStructItem() {
            return myStructItem;
        }

        @Override
        @Nullable
        public String getName() {
            return myStructItem.getName();
        }

        @Override
        @Nullable
        public RsSelfParameter getSelfParameter() {
            return null;
        }

        @Override
        @NotNull
        public List<Ty> getParameterTypes() {
            return RsFieldsOwnerExtUtil.getFieldTypes(myStructItem);
        }

        @Override
        @NotNull
        public Ty getRawReturnType() {
            return myStructItem.getDeclaredType();
        }

        @Override
        public boolean isAsync() {
            return false;
        }

        @Override
        @NotNull
        public KnownItems getKnownItems() {
            return KnownItems.getKnownItems(myStructItem);
        }

        @Override
        public boolean isActuallyUnsafe() {
            return false;
        }

        @Override
        @NotNull
        public RsAbstractableOwner getOwner() {
            return RsAbstractableOwner.Free;
        }

        @Override
        public boolean isVariadic() {
            return false;
        }

        @Override
        public boolean isIntrinsic() {
            return false;
        }

        @Override
        @NotNull
        public QueryAttributes<RsMetaItem> getQueryAttributes() {
            return RsAttrProcMacroOwnerExtUtil.getQueryAttributes(myStructItem);
        }
    }
}
