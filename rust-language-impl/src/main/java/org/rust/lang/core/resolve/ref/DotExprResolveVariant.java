/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.resolve.Namespace;
import org.rust.lang.core.resolve.ScopeEntry;
import org.rust.lang.core.types.ty.Ty;

import java.util.Set;

/**
 * Common interface for dot expression resolve variants (field access and method calls).
 */
public interface DotExprResolveVariant extends ScopeEntry {
    /** The receiver type after possible derefs performed */
    @Nonnull
    Ty getSelfTy();

    /** The number of {@code *} dereferences should be performed on receiver to match selfTy */
    int getDerefCount();

    @Nonnull
    @Override
    default Set<Namespace> getNamespaces() {
        return Namespace.VALUES; // Namespace does not matter in the case of dot expression
    }

    @Nonnull
    @Override
    default ScopeEntry copyWithNs(@Nonnull Set<Namespace> namespaces) {
        return this;
    }
}
