/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import jakarta.annotation.Nonnull;

public class MirBlockTailInfo {
    private final boolean tailResultIsIgnored;
    @Nonnull
    private final MirSpan span;

    public MirBlockTailInfo(boolean tailResultIsIgnored, @Nonnull MirSpan span) {
        this.tailResultIsIgnored = tailResultIsIgnored;
        this.span = span;
    }

    public boolean isTailResultIsIgnored() {
        return tailResultIsIgnored;
    }

    @Nonnull
    public MirSpan getSpan() {
        return span;
    }
}
