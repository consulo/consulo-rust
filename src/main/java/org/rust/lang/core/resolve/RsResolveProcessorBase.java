/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Base interface for resolve processors.
 *
 * @param <T> the type of scope entry this processor accepts
 */
public interface RsResolveProcessorBase<T extends ScopeEntry> {
    /**
     * Process a scope entry.
     *
     * @return {@code true} to stop further processing, {@code false} to continue search
     */
    boolean process(@NotNull T entry);

    /**
     * Indicates that processor is interested only in ScopeEntries with specified names.
     * Improves performance for Resolve2.
     *
     * @return the set of names this processor is interested in, or {@code null} for completion
     */
    @Nullable
    Set<String> getNames();

    default boolean acceptsName(@NotNull String name) {
        Set<String> names = getNames();
        return names == null || names.contains(name);
    }
}
