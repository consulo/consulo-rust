/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.extractFunction;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.types.ty.Ty;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class TyUtils {
    private TyUtils() {
    }

    @Nonnull
    public static Set<Ty> types(@Nonnull Ty ty) {
        Set<Ty> result = new HashSet<>();
        collectTypes(ty, result);
        return result;
    }

    private static void collectTypes(@Nonnull Ty ty, @Nonnull Set<Ty> result) {
        result.add(ty);
        for (Ty inner : ty.getTypeParameterValues().getTypes()) {
            collectTypes(inner, result);
        }
    }

    @Nonnull
    public static Set<Ty> dependTypes(@Nonnull Ty ty, @Nonnull Map<Ty, Set<Ty>> boundMap) {
        Set<Ty> result = new HashSet<>();
        collectDependTypes(ty, boundMap, result);
        return result;
    }

    private static void collectDependTypes(@Nonnull Ty ty, @Nonnull Map<Ty, Set<Ty>> boundMap, @Nonnull Set<Ty> result) {
        Set<Ty> bounds = boundMap.get(ty);
        if (bounds == null) return;
        for (Ty bound : bounds) {
            if (!result.contains(bound)) {
                result.add(bound);
                collectDependTypes(bound, boundMap, result);
            }
        }
    }
}
