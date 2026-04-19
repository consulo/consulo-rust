/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsPat;
import org.rust.lang.core.psi.RsPatRest;
import org.rust.lang.core.psi.RsPatTupleStruct;

public final class RsPatTupleStructUtil {
    private RsPatTupleStructUtil() {
    }

    @Nullable
    public static RsPatRest getPatRest(@NotNull RsPatTupleStruct patTupleStruct) {
        for (RsPat pat : patTupleStruct.getPatList()) {
            if (pat instanceof RsPatRest) return (RsPatRest) pat;
        }
        return null;
    }
}
