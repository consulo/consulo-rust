/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsTypeParameter;
import org.rust.lang.core.psi.RsWherePred;
import org.rust.lang.core.psi.ext.impl.RsFunctionUtil;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.Ty;

import java.util.List;
import java.util.stream.Collectors;

public abstract class RsFunctionSignatureConfig {
    @Nonnull
    private final RsFunction myFunction;

    public RsFunctionSignatureConfig(@Nonnull RsFunction function) {
        myFunction = function;
    }

    @Nonnull
    public RsFunction getFunction() {
        return myFunction;
    }

    @Nonnull
    protected String getTypeParametersText() {
        List<RsTypeParameter> typeParams = typeParameters();
        if (typeParams.isEmpty()) return "";
        return typeParams.stream()
            .map(tp -> tp.getText())
            .collect(Collectors.joining(", ", "<", ">"));
    }

    @Nonnull
    protected String getWhereClausesText() {
        List<RsWherePred> wherePredList = RsFunctionUtil.getWherePreds(myFunction);
        if (wherePredList.isEmpty()) return "";
        List<Ty> typeParams = typeParameters().stream()
            .map(tp -> tp.getDeclaredType())
            .collect(Collectors.toList());
        if (typeParams.isEmpty()) return "";
        List<RsWherePred> filtered = wherePredList.stream()
            .filter(wp -> wp.getTypeReference() != null && typeParams.contains(RsTypesUtil.getRawType(wp.getTypeReference())))
            .collect(Collectors.toList());
        if (filtered.isEmpty()) return "";
        return filtered.stream()
            .map(wp -> wp.getText())
            .collect(Collectors.joining(", ", " where ", ""));
    }

    @Nonnull
    protected abstract List<RsTypeParameter> typeParameters();
}
