/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.RsTypeAlias;
import org.rust.lang.core.resolve.KnownDerivableTrait;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.stubs.RsTraitItemStub;
import org.rust.lang.core.types.BoundElement;

import java.util.Collection;
import java.util.Map;

public final class RsTraitItemImplUtil {
    private RsTraitItemImplUtil() {
    }

    @Nullable
    public static String getLangAttribute(@NotNull RsTraitItem trait) {
        return RsDocAndAttributeOwnerUtil.getQueryAttributes(trait).getLangAttribute();
    }

    public static boolean isSizedTrait(@NotNull RsTraitItem trait) {
        return "sized".equals(getLangAttribute(trait));
    }

    public static boolean isAuto(@NotNull RsTraitItem trait) {
        RsTraitItemStub stub = RsPsiJavaUtil.getGreenStub(trait);
        if (stub != null) return stub.isAuto();
        return trait.getNode().findChildByType(RsElementTypes.AUTO) != null;
    }

    public static boolean isKnownDerivable(@NotNull RsTraitItem trait) {
        String name = trait.getName();
        if (name == null) return false;
        Map<String, KnownDerivableTrait> map = KnownItems.getKNOWN_DERIVABLE_TRAITS();
        KnownDerivableTrait derivableTrait = map.get(name);
        if (derivableTrait == null) return false;
        if (!derivableTrait.shouldUseHardcodedTraitDerive()) return false;
        return trait.equals(derivableTrait.findTrait(KnownItems.getKnownItems(trait)));
    }

    @Nullable
    public static RsTypeAlias findAssociatedType(@NotNull RsTraitItem trait, @NotNull String name) {
        Collection<RsTypeAlias> types = getAssociatedTypesTransitively(trait);
        for (RsTypeAlias type : types) {
            if (name.equals(type.getName())) {
                return type;
            }
        }
        return null;
    }

    @NotNull
    public static Collection<RsTypeAlias> getAssociatedTypesTransitively(@NotNull RsTraitItem trait) {
        return RsTraitItemUtil.getAssociatedTypesTransitively(new BoundElement<>(trait));
    }
}
