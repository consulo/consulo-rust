/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsPatBinding;
import org.rust.lang.core.psi.ext.RsElement;

import java.util.function.Consumer;

public final class ProcessLocalVariablesUtil {
    private ProcessLocalVariablesUtil() {
    }

    /** Feeds every local variable binding visible at {@code place} to {@code processor}. */
    public static void processLocalVariables(@Nonnull RsElement place, @Nonnull Consumer<RsPatBinding> processor) {
        NameResolution.processLocalVariables(place, processor);
    }
}
