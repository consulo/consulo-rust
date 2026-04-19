/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.resolve.Namespace;
import org.rust.lang.core.resolve.ScopeEntry;
import org.rust.lang.core.types.ty.Ty;

import java.util.Set;

/**
 * Common interface for dot expression resolve variants (field access and method calls).
 */
public interface DotExprResolveVariant extends ScopeEntry {
    /** The receiver type after possible derefs performed */
    @NotNull
    Ty getSelfTy();

    /** The number of {@code *} dereferences should be performed on receiver to match selfTy */
    int getDerefCount();

    @NotNull
    @Override
    default Set<Namespace> getNamespaces() {
        return Namespace.VALUES; // Namespace does not matter in the case of dot expression
    }

    @NotNull
    @Override
    default ScopeEntry copyWithNs(@NotNull Set<Namespace> namespaces) {
        return this;
    }
}
