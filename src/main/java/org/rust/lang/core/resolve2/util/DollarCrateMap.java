/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.crate.CratePersistentId;

import java.util.Collections;
import java.util.Map;

/**
 * Consider macro call with $crate in body:
 * {@code foo! { ... $crate ... }}
 * offset = keys in ranges
 */
public class DollarCrateMap {
    @NotNull
    public static final DollarCrateMap EMPTY = new DollarCrateMap(Collections.emptyMap());

    @NotNull
    private final Map<Integer, Integer> ranges;

    public DollarCrateMap(@NotNull Map<Integer, Integer> ranges) {
        this.ranges = ranges;
    }

    @Nullable
    public Integer mapOffsetFromExpansionToCallBody(int offset) {
        return ranges.get(offset);
    }
}
