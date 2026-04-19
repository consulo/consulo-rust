/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsConstParameter;
import org.rust.lang.core.psi.RsTypeParameter;
import org.rust.lang.core.psi.ext.RsGenericDeclaration;
import org.rust.lang.core.psi.ext.RsGenericDeclarationUtil;
import org.rust.lang.core.types.consts.CtConstParameter;
import org.rust.lang.core.types.ty.TyTypeParameter;

import java.util.ArrayList;
import java.util.List;

public final class TypeInferenceUtil {
    private TypeInferenceUtil() {}

    @NotNull
    public static List<TyTypeParameter> getGenerics(@NotNull RsGenericDeclaration element) {
        List<RsTypeParameter> params = RsGenericDeclarationUtil.getTypeParameters(element);
        List<TyTypeParameter> result = new ArrayList<>(params.size());
        for (RsTypeParameter p : params) result.add(TyTypeParameter.named(p));
        return result;
    }

    @NotNull
    public static List<CtConstParameter> getConstGenerics(@NotNull RsGenericDeclaration element) {
        List<RsConstParameter> params = RsGenericDeclarationUtil.getConstParameters(element);
        List<CtConstParameter> result = new ArrayList<>(params.size());
        for (RsConstParameter p : params) result.add(new CtConstParameter(p));
        return result;
    }

    @NotNull
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static List<Object> getPredicates(@NotNull RsGenericDeclaration element) {
        return (List) RsGenericDeclarationUtil.getPredicates(element);
    }
}
