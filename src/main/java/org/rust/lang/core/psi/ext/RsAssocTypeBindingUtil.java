/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsAssocTypeBinding;
import org.rust.lang.core.psi.RsPath;
import org.rust.lang.core.psi.RsTypeAlias;
import org.rust.lang.core.resolve.ref.RsPathReference;
import org.rust.lang.core.types.BoundElement;

/**
 * Extension functions for {@link RsAssocTypeBinding}.
 */
public final class RsAssocTypeBindingUtil {

    private RsAssocTypeBindingUtil() {
    }

    /**
     * Current grammar allows writing assoc type bindings in method calls, e.g.
     * {@code a.foo::<Item = i32>()}, so it's nullable.
     */
    @Nullable
    public static RsPath getParentPath(@NotNull RsAssocTypeBinding binding) {
        return PsiElementExt.ancestorStrict(binding, RsPath.class);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static BoundElement<RsTypeAlias> resolveToBoundAssocType(@NotNull RsAssocTypeBinding binding) {
        RsPathReference ref = binding.getPath().getReference();
        if (ref == null) return null;
        BoundElement<?> resolved = ref.advancedResolve();
        if (resolved == null) return null;
        return resolved.downcast(RsTypeAlias.class);
    }
}
