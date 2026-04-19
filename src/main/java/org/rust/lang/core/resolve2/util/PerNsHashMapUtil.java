/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2.util;

import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.resolve.Namespace;
import org.rust.lang.core.resolve2.PerNs;
import org.rust.lang.core.resolve2.VisItem;

public final class PerNsHashMapUtil {

    private PerNsHashMapUtil() {}

    @Nullable
    public static Pair<VisItem, Namespace> asSingleVisItem(@NotNull PerNs perNs) {
        int total = perNs.getTypes().length + perNs.getValues().length + perNs.getMacros().length;
        if (total != 1) return null;
        if (perNs.getTypes().length == 1) return new Pair<>(perNs.getTypes()[0], Namespace.Types);
        if (perNs.getValues().length == 1) return new Pair<>(perNs.getValues()[0], Namespace.Values);
        if (perNs.getMacros().length == 1) return new Pair<>(perNs.getMacros()[0], Namespace.Macros);
        return null;
    }
}
