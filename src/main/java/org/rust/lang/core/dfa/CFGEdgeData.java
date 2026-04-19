/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.dfa;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.ext.RsElement;

import java.util.List;

public class CFGEdgeData {
    @NotNull
    private final List<RsElement> exitingScopes;

    public CFGEdgeData(@NotNull List<RsElement> exitingScopes) {
        this.exitingScopes = exitingScopes;
    }

    @NotNull
    public List<RsElement> getExitingScopes() {
        return exitingScopes;
    }
}
