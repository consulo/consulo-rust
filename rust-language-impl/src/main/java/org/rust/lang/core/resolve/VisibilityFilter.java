/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.ext.RsElement;

/**
 * Functional interface for checking visibility of a scope entry from a given context.
 */
@FunctionalInterface
public interface VisibilityFilter {
    @Nonnull
    VisibilityStatus apply(@Nonnull RsElement context, @Nullable Object lazyModInfo);
}
