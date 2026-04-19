/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.rust.lang.core.psi.ext.PsiElementUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsMembers;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.RsTypeAlias;
import org.rust.lang.core.types.BoundElement;

import java.util.Collections;
import java.util.List;

public final class RsTraitOrImplUtil {
    private RsTraitOrImplUtil() {
    }

    @NotNull
    public static List<RsAbstractable> getExpandedMembers(@NotNull RsTraitOrImpl traitOrImpl) {
        RsMembers members = traitOrImpl.getMembers();
        if (members == null) return Collections.emptyList();
        return RsMembersUtil.getExpandedMembers(members);
    }

    @NotNull
    public static List<RsAbstractable> getExplicitMembers(@NotNull RsTraitOrImpl traitOrImpl) {
        RsMembers members = traitOrImpl.getMembers();
        if (members == null) return Collections.emptyList();
        return PsiElementUtil.stubChildrenOfType(members, RsAbstractable.class);
    }

    @Nullable
    public static BoundElement<RsTraitItem> getImplementedTrait(@NotNull RsTraitOrImpl traitOrImpl) {
        return traitOrImpl.getImplementedTrait();
    }
}
