/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsRefLikeType;
import org.rust.lang.core.stubs.RsRefLikeTypeStub;
import org.rust.lang.core.types.ty.Mutability;

public final class RsRefLikeTypeUtil {
    private RsRefLikeTypeUtil() {
    }

    @NotNull
    public static Mutability getMutability(@NotNull RsRefLikeType refLikeType) {
        Object stub = RsPsiJavaUtil.getGreenStub(refLikeType);
        if (stub instanceof RsRefLikeTypeStub) {
            return Mutability.valueOf(((RsRefLikeTypeStub) stub).isMut());
        }
        return Mutability.valueOf(refLikeType.getMut() != null);
    }

    public static boolean isRef(@NotNull RsRefLikeType refLikeType) {
        Object stub = RsPsiJavaUtil.getGreenStub(refLikeType);
        if (stub instanceof RsRefLikeTypeStub) {
            return ((RsRefLikeTypeStub) stub).isRef();
        }
        return refLikeType.getAnd() != null;
    }

    public static boolean isPointer(@NotNull RsRefLikeType refLikeType) {
        Object stub = RsPsiJavaUtil.getGreenStub(refLikeType);
        if (stub instanceof RsRefLikeTypeStub) {
            return ((RsRefLikeTypeStub) stub).isPointer();
        }
        return refLikeType.getMul() != null;
    }
}
