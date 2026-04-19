/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsTypeParameterList;
import org.rust.lang.core.psi.RsWhereClause;

public interface RsGenericDeclaration extends RsElement {
    @Nullable
    RsTypeParameterList getTypeParameterList();

    @Nullable
    RsWhereClause getWhereClause();

    default java.util.List<org.rust.lang.core.psi.RsTypeParameter> getTypeParameters() {
        RsTypeParameterList list = getTypeParameterList();
        return list != null ? list.getTypeParameterList() : java.util.Collections.emptyList();
    }

    default java.util.List<org.rust.lang.core.psi.RsLifetimeParameter> getLifetimeParameters() {
        RsTypeParameterList list = getTypeParameterList();
        return list != null ? list.getLifetimeParameterList() : java.util.Collections.emptyList();
    }

    default java.util.List<org.rust.lang.core.psi.RsConstParameter> getConstParameters() {
        RsTypeParameterList list = getTypeParameterList();
        return list != null ? list.getConstParameterList() : java.util.Collections.emptyList();
    }
}
