/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.RsElement;

/**
 * Functional interface for checking visibility of a scope entry from a given context.
 */
@FunctionalInterface
public interface VisibilityFilter {
    @NotNull
    VisibilityStatus apply(@NotNull RsElement context, @Nullable Object lazyModInfo);
}
