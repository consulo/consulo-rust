/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsPatFieldFull;
import org.rust.lang.core.psi.RsPatStruct;

public final class RsPatFieldFullUtil {
    private RsPatFieldFullUtil() {
    }

    @NotNull
    public static RsPatStruct getParentStructPattern(@NotNull RsPatFieldFull patFieldFull) {
        return RsPsiJavaUtil.ancestorStrict(patFieldFull, RsPatStruct.class);
    }
}
