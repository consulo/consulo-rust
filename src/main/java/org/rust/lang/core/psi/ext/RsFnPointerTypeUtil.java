/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsFnPointerType;
import org.rust.lang.core.psi.RsValueParameter;
import org.rust.lang.core.stubs.RsFnPointerTypeStub;

import java.util.Collections;
import java.util.List;

public final class RsFnPointerTypeUtil {
    private RsFnPointerTypeUtil() {
    }

    @NotNull
    public static List<RsValueParameter> getValueParameters(@NotNull RsFnPointerType fnPtr) {
        if (fnPtr.getValueParameterList() == null) return Collections.emptyList();
        return fnPtr.getValueParameterList().getValueParameterList();
    }

    public static boolean isUnsafe(@NotNull RsFnPointerType fnPtr) {
        Object stub = RsPsiJavaUtil.getGreenStub(fnPtr);
        if (stub instanceof RsFnPointerTypeStub) {
            return ((RsFnPointerTypeStub) stub).isUnsafe();
        }
        return fnPtr.getUnsafe() != null;
    }

    public static boolean isExtern(@NotNull RsFnPointerType fnPtr) {
        Object stub = RsPsiJavaUtil.getGreenStub(fnPtr);
        if (stub instanceof RsFnPointerTypeStub) {
            return ((RsFnPointerTypeStub) stub).isExtern();
        }
        return fnPtr.getExternAbi() != null;
    }

    @Nullable
    public static String getAbiName(@NotNull RsFnPointerType fnPtr) {
        Object stub = RsPsiJavaUtil.getGreenStub(fnPtr);
        if (stub instanceof RsFnPointerTypeStub) {
            return ((RsFnPointerTypeStub) stub).getAbiName();
        }
        if (fnPtr.getExternAbi() != null && fnPtr.getExternAbi().getLitExpr() != null) {
            return RsLitExprUtil.getStringValue(fnPtr.getExternAbi().getLitExpr());
        }
        return null;
    }
}
