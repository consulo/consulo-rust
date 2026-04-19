/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.RsTraitRef;
import org.rust.lang.core.resolve.ref.RsPathReference;
import org.rust.lang.core.types.BoundElement;

public final class RsTraitRefUtil {
    private RsTraitRefUtil() {
    }

    @Nullable
    public static RsTraitItem resolveToTrait(@NotNull RsTraitRef traitRef) {
        RsPathReference ref = traitRef.getPath().getReference();
        if (ref == null) return null;
        PsiElement resolved = ref.resolve();
        return resolved instanceof RsTraitItem ? (RsTraitItem) resolved : null;
    }

    @Nullable
    public static BoundElement<RsTraitItem> resolveToBoundTrait(@NotNull RsTraitRef traitRef) {
        RsPathReference ref = traitRef.getPath().getReference();
        if (ref == null) return null;
        BoundElement<?> bound = ref.advancedResolve();
        if (bound == null) return null;
        return bound.downcast(RsTraitItem.class);
    }

    public static boolean isParenthesized(@NotNull RsTraitRef traitRef) {
        return traitRef.getPath().getValueParameterList() != null;
    }

}
