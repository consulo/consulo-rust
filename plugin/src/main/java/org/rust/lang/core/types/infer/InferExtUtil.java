/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

public final class InferExtUtil {
    private InferExtUtil() {
    }

    @org.jetbrains.annotations.Nullable
    public static org.rust.lang.core.types.ty.Ty selfType(@org.jetbrains.annotations.NotNull org.rust.lang.core.psi.RsFunction function) {
        return RsTypeInferenceWalkerHelper.getSelfType(function);
    }

    @org.jetbrains.annotations.NotNull
    public static org.rust.lang.core.types.ty.Ty typeOfValue(@org.jetbrains.annotations.NotNull org.rust.lang.core.psi.RsSelfParameter self) {
        return RsTypeInferenceWalkerHelper.getTypeOfSelfParameter(self);
    }
}
