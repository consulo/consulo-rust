/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsMembers;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.RsTypeAlias;
import org.rust.lang.core.types.BoundElement;

import java.util.Collection;

public interface RsTraitOrImpl extends RsItemElement, RsGenericDeclaration, RsAttrProcMacroOwner,
    RsInnerAttributeOwner, RsTypeDeclarationElement {

    @Nullable
    RsMembers getMembers();

    @Nullable
    BoundElement<RsTraitItem> getImplementedTrait();

    @NotNull
    Collection<RsTypeAlias> getAssociatedTypesTransitively();
}
