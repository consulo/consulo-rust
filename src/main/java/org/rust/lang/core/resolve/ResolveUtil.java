/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.function.Consumer;

public final class ResolveUtil {
    private ResolveUtil() {
    }

    /**
     * Creates an {@link RsResolveProcessor} from a consumer that does not signal early termination.
     * The returned processor always returns {@code false} from {@link RsResolveProcessor#process}.
     */
    @NotNull
    public static RsResolveProcessor createProcessor(@NotNull Consumer<ScopeEntry> consumer) {
        return new RsResolveProcessor() {
            @Override
            public boolean process(@NotNull ScopeEntry entry) {
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
