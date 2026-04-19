/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsBlockFields;
import org.rust.lang.core.psi.RsTupleFields;
import org.rust.lang.core.psi.ext.RsFieldDecl;

public interface RsFieldsOwner extends RsElement, RsNameIdentifierOwner, RsQualifiedNamedElement {
    @Nullable
    RsBlockFields getBlockFields();

    @Nullable
    RsTupleFields getTupleFields();

    @org.jetbrains.annotations.NotNull
    default java.util.List<RsFieldDecl> getFields() {
        return RsFieldsOwnerUtil.getFields(this);
    }
}
