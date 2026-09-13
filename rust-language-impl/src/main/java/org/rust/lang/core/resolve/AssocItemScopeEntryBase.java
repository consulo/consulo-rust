/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.ext.RsAbstractable;
import org.rust.lang.core.types.ty.Ty;

/**
 * Interface for associated item scope entries.
 */
public interface AssocItemScopeEntryBase<T extends RsAbstractable> extends ScopeEntry {
    @Nonnull
    T getElement();

    @Nullable
    Ty getSelfTy();

    @Nonnull
    TraitImplSource getSource();
}
