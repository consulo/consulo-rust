/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.*;

public final class RsItemElementUtil {
    private RsItemElementUtil() {
    }

    @NotNull
    public static String getItemKindName(@NotNull RsItemElement item) {
        if (item instanceof RsMod || item instanceof RsModDeclItem) return "module";
        if (item instanceof RsFunction) return "function";
        if (item instanceof RsConstant) {
            switch (RsConstantUtil.getKind((RsConstant) item)) {
                case STATIC:
                case MUT_STATIC:
                    return "static";
                case CONST:
                    return "constant";
            }
        }
        if (item instanceof RsStructItem) {
            switch (RsStructItemUtil.getKind((RsStructItem) item)) {
                case STRUCT: return "struct";
                case UNION: return "union";
            }
        }
        if (item instanceof RsEnumItem) return "enum";
        if (item instanceof RsTraitItem) return "trait";
        if (item instanceof RsTraitAlias) return "trait alias";
        if (item instanceof RsTypeAlias) return "type alias";
        if (item instanceof RsImplItem) return "impl";
        if (item instanceof RsUseItem) return "use item";
        if (item instanceof RsForeignModItem) return "foreign module";
        if (item instanceof RsExternCrateItem) return "extern crate";
        if (item instanceof RsMacro2) return "macro";
        return "item";
    }

    @NotNull
    public static String getArticle(@NotNull RsItemElement item) {
        if (item instanceof RsImplItem) return "an";
        return "a";
    }

    @NotNull
    public static PsiElement getItemDefKeyword(@NotNull RsItemElement item) {
        if (item instanceof RsModItem) return ((RsModItem) item).getMod();
        if (item instanceof RsModDeclItem) return ((RsModDeclItem) item).getMod();
        if (item instanceof RsFunction) return ((RsFunction) item).getFn();
        if (item instanceof RsConstant) {
            PsiElement kw = ((RsConstant) item).getConst();
            if (kw == null) kw = ((RsConstant) item).getStatic();
            if (kw == null) throw new IllegalStateException("unknown constant type");
            return kw;
        }
        if (item instanceof RsStructItem) {
            PsiElement kw = ((RsStructItem) item).getStruct();
            if (kw == null) kw = RsStructItemUtil.getUnion((RsStructItem) item);
            if (kw == null) throw new IllegalStateException("unknown struct type");
            return kw;
        }
        if (item instanceof RsEnumItem) return ((RsEnumItem) item).getEnum();
        if (item instanceof RsTraitItem) return ((RsTraitItem) item).getTrait();
        if (item instanceof RsTraitAlias) return ((RsTraitAlias) item).getTrait();
        if (item instanceof RsTypeAlias) return ((RsTypeAlias) item).getTypeKw();
        if (item instanceof RsImplItem) return ((RsImplItem) item).getImpl();
        if (item instanceof RsUseItem) return ((RsUseItem) item).getUse();
        if (item instanceof RsForeignModItem) return ((RsForeignModItem) item).getExternAbi().getExtern();
        if (item instanceof RsExternCrateItem) return ((RsExternCrateItem) item).getExtern();
        if (item instanceof RsMacro2) return ((RsMacro2) item).getMacroKw();
        throw new IllegalStateException("unknown item type: " + item.getClass());
    }
}
