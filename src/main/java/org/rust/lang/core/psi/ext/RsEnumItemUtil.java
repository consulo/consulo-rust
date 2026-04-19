/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsEnumBody;
import org.rust.lang.core.psi.RsEnumItem;
import org.rust.lang.core.psi.RsEnumVariant;
import org.rust.lang.core.psi.RsMetaItem;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.stubs.RsEnumItemStub;
import org.rust.lang.core.stubs.RsEnumVariantStub;
import org.rust.lang.core.types.ty.TyInteger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RsEnumItemUtil {
    private RsEnumItemUtil() {
    }

    public static boolean isStdOptionOrResult(@NotNull RsEnumItem enumItem) {
        KnownItems knownItems = KnownItems.getKnownItems(enumItem);
        RsStructOrEnumItemElement option = knownItems.getOption();
        RsStructOrEnumItemElement result = knownItems.getResult();
        return enumItem.equals(option) || enumItem.equals(result);
    }

    @NotNull
    public static List<RsEnumVariant> getVariants(@NotNull RsEnumItem enumItem) {
        RsEnumBody body = enumItem.getEnumBody();
        if (body == null) return Collections.emptyList();
        return body.getEnumVariantList();
    }

    @NotNull
    public static List<RsEnumVariantStub> getVariants(@NotNull RsEnumItemStub stub) {
        com.intellij.psi.stubs.StubElement<?> enumBody = stub.getEnumBody();
        if (enumBody == null) return Collections.emptyList();
        List<RsEnumVariantStub> result = new ArrayList<>();
        for (Object child : enumBody.getChildrenStubs()) {
            if (child instanceof RsEnumVariantStub) {
                result.add((RsEnumVariantStub) child);
            }
        }
        return result;
    }

    @NotNull
    public static TyInteger getReprType(@NotNull RsEnumItem enumItem) {
        QueryAttributes<RsMetaItem> attrs = RsDocAndAttributeOwnerUtil.getQueryAttributes(enumItem);
        for (RsMetaItem repr : (Iterable<RsMetaItem>) () -> attrs.getReprAttributes().iterator()) {
            if (repr.getMetaItemArgs() != null) {
                List<RsMetaItem> metaList = repr.getMetaItemArgs().getMetaItemList();
                TyInteger lastFound = null;
                for (RsMetaItem meta : metaList) {
                    String name = RsMetaItemUtil.getName(meta);
                    if (name != null && TyInteger.NAMES.contains(name)) {
                        TyInteger ty = tyIntegerFromName(name);
                        if (ty != null) {
                            lastFound = ty;
                        }
                    }
                }
                if (lastFound != null) return lastFound;
            }
        }
        return TyInteger.ISize.INSTANCE;
    }

    @org.jetbrains.annotations.Nullable
    private static TyInteger tyIntegerFromName(@NotNull String name) {
        switch (name) {
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
            default: return null;
        }
    }
}
