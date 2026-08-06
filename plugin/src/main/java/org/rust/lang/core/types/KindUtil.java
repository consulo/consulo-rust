/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types;

import java.util.Collection;

public final class KindUtil {
    public static final int HAS_TY_INFER_MASK = 1;
    public static final int HAS_TY_TYPE_PARAMETER_MASK = 2;
    public static final int HAS_TY_PROJECTION_MASK = 4;
    public static final int HAS_RE_EARLY_BOUND_MASK = 8;
    public static final int HAS_CT_INFER_MASK = 16;
    public static final int HAS_CT_PARAMETER_MASK = 32;
    public static final int HAS_CT_UNEVALUATED_MASK = 64;
    public static final int HAS_TY_OPAQUE_MASK = 128;
    public static final int HAS_TY_PLACEHOLDER_MASK = 256;

    private KindUtil() {
    }

    public static int mergeFlags(Collection<? extends Kind> kinds) {
        int result = 0;
        for (Kind kind : kinds) {
            result |= kind.getFlags();
        }
        return result;
    }

    public static int mergeElementFlags(BoundElement<?> element) {
        return mergeFlags(element.getSubst().getKinds()) | mergeFlags(element.getAssoc().values());
    }

    public static int mergeElementFlags(Collection<BoundElement<?>> elements) {
        int result = 0;
        for (BoundElement<?> element : elements) {
            result |= mergeElementFlags(element);
        }
        return result;
    }
}
