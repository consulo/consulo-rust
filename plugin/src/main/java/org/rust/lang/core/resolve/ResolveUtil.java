/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Set;
import java.util.function.Consumer;

public final class ResolveUtil {
    private ResolveUtil() {
    }

    /**
     * Creates an {@link RsResolveProcessor} from a consumer that does not signal early termination.
     * The returned processor always returns {@code false} from {@link RsResolveProcessor#process}.
     */
    @Nonnull
    public static RsResolveProcessor createProcessor(@Nonnull Consumer<ScopeEntry> consumer) {
        return new RsResolveProcessor() {
            @Override
            public boolean process(@Nonnull ScopeEntry entry) {
                consumer.accept(entry);
                return false;
            }

            @Nullable
            @Override
            public Set<String> getNames() {
                return null;
            }
        };
    }
}
