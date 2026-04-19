/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import org.jetbrains.annotations.NotNull;

public class MirBlockTailInfo {
    private final boolean tailResultIsIgnored;
    @NotNull
    private final MirSpan span;

    public MirBlockTailInfo(boolean tailResultIsIgnored, @NotNull MirSpan span) {
        this.tailResultIsIgnored = tailResultIsIgnored;
        this.span = span;
    }

    public boolean isTailResultIsIgnored() {
        return tailResultIsIgnored;
    }

    @NotNull
    public MirSpan getSpan() {
        return span;
    }
}
