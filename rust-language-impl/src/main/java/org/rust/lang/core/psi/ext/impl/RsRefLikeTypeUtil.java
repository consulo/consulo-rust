/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsRefLikeType;
import org.rust.lang.core.stubs.RsRefLikeTypeStub;
import org.rust.lang.core.types.ty.Mutability;
import org.rust.lang.core.psi.ext.*;

public final class RsRefLikeTypeUtil {
    private RsRefLikeTypeUtil() {
    }

    @Nonnull
    public static Mutability getMutability(@Nonnull RsRefLikeType refLikeType) {
        Object stub = RsPsiJavaUtil.getGreenStub(refLikeType);
        if (stub instanceof RsRefLikeTypeStub) {
            return Mutability.valueOf(((RsRefLikeTypeStub) stub).isMut());
        }
        return Mutability.valueOf(refLikeType.getMut() != null);
    }

    public static boolean isRef(@Nonnull RsRefLikeType refLikeType) {
        Object stub = RsPsiJavaUtil.getGreenStub(refLikeType);
        if (stub instanceof RsRefLikeTypeStub) {
            return ((RsRefLikeTypeStub) stub).isRef();
        }
        return refLikeType.getAnd() != null;
    }

    public static boolean isPointer(@Nonnull RsRefLikeType refLikeType) {
        Object stub = RsPsiJavaUtil.getGreenStub(refLikeType);
        if (stub instanceof RsRefLikeTypeStub) {
            return ((RsRefLikeTypeStub) stub).isPointer();
        }
        return refLikeType.getMul() != null;
    }
}
