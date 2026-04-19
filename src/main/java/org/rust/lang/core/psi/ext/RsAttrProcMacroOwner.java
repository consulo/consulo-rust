/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ProcMacroAttribute;
import org.rust.lang.core.psi.RsMetaItem;
import org.rust.lang.core.stubs.common.RsAttrProcMacroOwnerPsiOrStub;

/**
 * A common interface for PSI elements that can hold attribute or derive procedural macro attributes.
 *
 * @see org.rust.lang.core.stubs.RsAttrProcMacroOwnerStub
 */
public interface RsAttrProcMacroOwner extends RsOuterAttributeOwner, RsAttrProcMacroOwnerPsiOrStub<RsMetaItem> {

    /**
     * @see ProcMacroAttribute
     */
    @Nullable
    default ProcMacroAttribute<RsMetaItem> getProcMacroAttribute() {
        return ProcMacroAttribute.getProcMacroAttribute(this);
    }

    /**
     * @see ProcMacroAttribute
     */
    @Nullable
    default ProcMacroAttribute<RsMetaItem> getProcMacroAttributeWithDerives() {
        return ProcMacroAttribute.getProcMacroAttribute(this, RsDocAndAttributeOwnerUtil.getAttributeStub(this), null, true, false);
    }
}
