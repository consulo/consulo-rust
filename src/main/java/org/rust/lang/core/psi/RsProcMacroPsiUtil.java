/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.RsPsiPattern;
import org.rust.lang.core.macros.proc.ProcMacroApplicationService;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.psi.ext.RsAttrUtil;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.stubs.*;
import org.rust.lang.core.stubs.common.RsAttrProcMacroOwnerPsiOrStub;
import org.rust.lang.core.stubs.common.RsMetaItemPsiOrStub;

public final class RsProcMacroPsiUtil {
    public static final RsProcMacroPsiUtil INSTANCE = new RsProcMacroPsiUtil();

    private RsProcMacroPsiUtil() {}

    public static boolean canBeInProcMacroCallBody(@NotNull PsiElement psiElement) {
        if (!ProcMacroApplicationService.isAnyEnabled()) return false;
        PsiElement current = psiElement;
        while (current != null) {
            if (current instanceof RsAttrProcMacroOwner owner && canHaveProcMacroCall(owner)) {
                return true;
            }
            current = current.getParent();
        }
        return false;
    }

    private static boolean canHaveProcMacroCall(@NotNull RsAttrProcMacroOwner item) {
        boolean checkDerives = item instanceof RsStructOrEnumItemElement;
        for (var meta : RsDocAndAttributeOwnerUtil.getTraversedRawAttributes(item, false).getMetaItems()) {
            if (canBeProcMacroAttributeCall(meta, CustomAttributes.EMPTY) && ProcMacroApplicationService.isAttrEnabled()) return true;
            if (checkDerives && "derive".equals(RsMetaItemUtil.getName(meta))) {
                var args = meta.getMetaItemArgs();
                if (args != null) {
                    for (var m : args.getMetaItemList()) {
                        if (canBeCustomDerive(m)) {
                            return ProcMacroApplicationService.isDeriveEnabled();
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Returns true if this metaItem can be a custom (non-std) derive macro call.
     */
    public static boolean canBeCustomDerive(@NotNull RsMetaItem metaItem) {
        boolean isDerive = RsPsiPattern.derivedTraitMetaItem.accepts(metaItem);
        return isDerive && canBeCustomDeriveWithoutContextCheck(metaItem);
    }

    public static boolean canBeCustomDeriveWithoutContextCheck(@NotNull RsMetaItemPsiOrStub metaItem) {
        String name = RsMetaItemUtil.getName(metaItem);
        var info = KnownItems.getKNOWN_DERIVABLE_TRAITS().get(name);
        return info == null || !info.isStd();
    }

    public static boolean canBeProcMacroAttributeCall(
        @NotNull RsMetaItem metaItem,
        @NotNull CustomAttributes customAttrs
    ) {
        ProcessingContext context = new ProcessingContext();
        if (!RsMetaItemUtil.isRootMetaItem(metaItem, context)) return false;
        var containingAttr = context.get(RsPsiPattern.META_ITEM_ATTR);
        if (!(containingAttr instanceof RsOuterAttr)) return false;
        if (!(RsAttrUtil.getOwner((RsOuterAttr) containingAttr) instanceof RsAttrProcMacroOwner)) return false;

        return canBeProcMacroAttributeCallWithoutContextCheck(metaItem, customAttrs);
    }

    public static boolean canBeProcMacroAttributeCallWithoutContextCheck(
        @NotNull RsMetaItemPsiOrStub metaItem,
        @NotNull CustomAttributes customAttrs
    ) {
        String name = RsMetaItemUtil.getName(metaItem);
        if (name == null) {
            // A possible multi-segment path #[foo::bar]
            var path = metaItem.getPath();
            if (path == null) return false;
            var basePath = RsPathUtil.basePath(path);
            if (basePath == null) return false;
            String base = basePath.getReferenceName();
            if (base == null) return false;
            return !BuiltinAttributes.RS_BUILTIN_TOOL_ATTRIBUTES.contains(base) && !customAttrs.customTools().contains(base);
        } else {
            return !BuiltinAttributes.RS_BUILTIN_ATTRIBUTES.containsKey(name) && !customAttrs.customAttrs().contains(name);
        }
    }

    /**
     * Returns true if this metaItem can be an attribute proc macro call or
     * a custom (non-std) derive macro call.
     */
    public static boolean canBeProcMacroCall(@NotNull RsMetaItem metaItem) {
        return canBeCustomDerive(metaItem) || canBeProcMacroAttributeCall(metaItem, CustomAttributes.EMPTY);
    }

    /**
     * Sometimes we want to ignore a proc macro attribute, i.e. leave the item as is.
     */
    public static boolean canFallBackAttrMacroToOriginalItem(@NotNull RsAttrProcMacroOwnerStub item) {
        return item instanceof RsImplItemStub || (item instanceof RsNamedStub
            && !(item instanceof RsEnumItemStub)
            && !(item instanceof RsModItemStub)
            && !(item instanceof RsMacroStub)
            && !(item instanceof RsMacro2Stub));
    }

    /** @see #canFallBackAttrMacroToOriginalItem(RsAttrProcMacroOwnerStub) */
    public static boolean canFallBackAttrMacroToOriginalItem(@NotNull RsAttrProcMacroOwner item) {
        return item instanceof RsImplItem || (item instanceof RsNamedElement
            && !(item instanceof RsEnumItem)
            && !(item instanceof RsModItem)
            && !(item instanceof RsMacroDefinitionBase));
    }

    public static boolean canOwnDeriveAttrs(@NotNull RsAttrProcMacroOwnerPsiOrStub<?> item) {
        return item instanceof RsStructOrEnumItemElement
            || item instanceof RsStructItemStub
            || item instanceof RsEnumItemStub;
    }
}
