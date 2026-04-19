/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsRetType;
import org.rust.lang.core.psi.RsValueParameterList;

/**
 * Represents {@link org.rust.lang.core.psi.RsFunction} or {@link org.rust.lang.core.psi.RsLambdaExpr}.
 */
public interface RsFunctionOrLambda extends RsOuterAttributeOwner {
    @Nullable
    RsValueParameterList getValueParameterList();

    @Nullable
    RsRetType getRetType();

    default java.util.List<org.rust.lang.core.psi.RsValueParameter> getValueParameters() {
        RsValueParameterList list = getValueParameterList();
        return list != null ? list.getValueParameterList() : java.util.Collections.emptyList();
    }

    default java.util.List<org.rust.lang.core.psi.RsValueParameter> getRawValueParameters() {
        return getValueParameters();
    }

    @Nullable
    default org.rust.lang.core.psi.RsSelfParameter getSelfParameter() {
        RsValueParameterList list = getValueParameterList();
        return list != null ? list.getSelfParameter() : null;
    }

    default boolean isMethod() {
        return getSelfParameter() != null;
    }
}
