/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsSelfParameter;
import org.rust.lang.core.stubs.RsSelfParameterStub;
import org.rust.lang.core.types.ty.Mutability;

public final class RsSelfParameterExtUtil {
    private RsSelfParameterExtUtil() {
    }

    @NotNull
    public static Mutability getMutability(@NotNull RsSelfParameter self) {
        RsSelfParameterStub stub = RsPsiJavaUtil.getGreenStub(self);
        boolean isMut = stub != null ? stub.isMut() : self.getMut() != null;
        return Mutability.valueOf(isMut);
    }

    public static boolean isRef(@NotNull RsSelfParameter self) {
        RsSelfParameterStub stub = RsPsiJavaUtil.getGreenStub(self);
        return stub != null ? stub.isRef() : self.getAnd() != null;
    }

    public static boolean isExplicitType(@NotNull RsSelfParameter self) {
        RsSelfParameterStub stub = RsPsiJavaUtil.getGreenStub(self);
        return stub != null ? stub.isExplicitType() : self.getColon() != null;
    }

    @NotNull
    public static RsFunction getParentFunction(@NotNull RsSelfParameter self) {
        RsFunction fn = RsPsiJavaUtil.ancestorStrict(self, RsFunction.class);
        assert fn != null;
        return fn;
    }
}
