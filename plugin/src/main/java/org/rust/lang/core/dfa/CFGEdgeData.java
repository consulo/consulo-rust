/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.dfa;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.ext.RsElement;

import java.util.List;

public class CFGEdgeData {
    @Nonnull
    private final List<RsElement> exitingScopes;

    public CFGEdgeData(@Nonnull List<RsElement> exitingScopes) {
        this.exitingScopes = exitingScopes;
    }

    @Nonnull
    public List<RsElement> getExitingScopes() {
        return exitingScopes;
    }
}
