/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.RsAbstractable;
import org.rust.lang.core.types.ty.Ty;

/**
 * Interface for associated item scope entries.
 */
public interface AssocItemScopeEntryBase<T extends RsAbstractable> extends ScopeEntry {
    @NotNull
    T getElement();

    @Nullable
    Ty getSelfTy();

    @NotNull
    TraitImplSource getSource();
}
