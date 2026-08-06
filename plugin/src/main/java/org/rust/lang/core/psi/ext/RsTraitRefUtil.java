/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.RsTraitRef;
import org.rust.lang.core.resolve.ref.RsPathReference;
import org.rust.lang.core.types.BoundElement;

public final class RsTraitRefUtil {
    private RsTraitRefUtil() {
    }

    @Nullable
    public static RsTraitItem resolveToTrait(@Nonnull RsTraitRef traitRef) {
        RsPathReference ref = traitRef.getPath().getReference();
        if (ref == null) return null;
        PsiElement resolved = ref.resolve();
        return resolved instanceof RsTraitItem ? (RsTraitItem) resolved : null;
    }

    @Nullable
    public static BoundElement<RsTraitItem> resolveToBoundTrait(@Nonnull RsTraitRef traitRef) {
        RsPathReference ref = traitRef.getPath().getReference();
        if (ref == null) return null;
        BoundElement<?> bound = ref.advancedResolve();
        if (bound == null) return null;
        return bound.downcast(RsTraitItem.class);
    }

    public static boolean isParenthesized(@Nonnull RsTraitRef traitRef) {
        return traitRef.getPath().getValueParameterList() != null;
    }

}
