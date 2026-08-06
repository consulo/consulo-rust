/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsSelfParameter;
import org.rust.lang.core.stubs.RsSelfParameterStub;
import org.rust.lang.core.types.ty.Mutability;

public final class RsSelfParameterUtil {
    private RsSelfParameterUtil() {
    }

    @Nonnull
    public static Mutability getMutability(@Nonnull RsSelfParameter self) {
        RsSelfParameterStub stub = RsPsiJavaUtil.getGreenStub(self);
        boolean isMut = stub != null ? stub.isMut() : self.getMut() != null;
        return Mutability.valueOf(isMut);
    }

    public static boolean isRef(@Nonnull RsSelfParameter self) {
        RsSelfParameterStub stub = RsPsiJavaUtil.getGreenStub(self);
        return stub != null ? stub.isRef() : self.getAnd() != null;
    }

    public static boolean isExplicitType(@Nonnull RsSelfParameter self) {
        RsSelfParameterStub stub = RsPsiJavaUtil.getGreenStub(self);
        return stub != null ? stub.isExplicitType() : self.getColon() != null;
    }

    @Nonnull
    public static RsFunction getParentFunction(@Nonnull RsSelfParameter self) {
        RsFunction fn = RsPsiJavaUtil.ancestorStrict(self, RsFunction.class);
        assert fn != null;
        return fn;
    }
}
