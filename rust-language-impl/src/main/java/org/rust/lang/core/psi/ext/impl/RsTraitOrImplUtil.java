/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import org.rust.lang.core.psi.ext.impl.PsiElementUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsMembers;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.types.BoundElement;

import java.util.Collections;
import java.util.List;
import org.rust.lang.core.psi.ext.*;

public final class RsTraitOrImplUtil {
    private RsTraitOrImplUtil() {
    }

    @Nonnull
    public static List<RsAbstractable> getExpandedMembers(@Nonnull RsTraitOrImpl traitOrImpl) {
        RsMembers members = traitOrImpl.getMembers();
        if (members == null) return Collections.emptyList();
        return RsMembersUtil.getExpandedMembers(members);
    }

    @Nonnull
    public static List<RsAbstractable> getExplicitMembers(@Nonnull RsTraitOrImpl traitOrImpl) {
        RsMembers members = traitOrImpl.getMembers();
        if (members == null) return Collections.emptyList();
        return PsiElementUtil.stubChildrenOfType(members, RsAbstractable.class);
    }

    @Nullable
    public static BoundElement<RsTraitItem> getImplementedTrait(@Nonnull RsTraitOrImpl traitOrImpl) {
        return traitOrImpl.getImplementedTrait();
    }
}
