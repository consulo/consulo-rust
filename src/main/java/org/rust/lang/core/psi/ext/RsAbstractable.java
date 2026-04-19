/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.rust.lang.core.macros.RsExpandedElement;

import javax.swing.Icon;

public interface RsAbstractable extends RsNameIdentifierOwner, RsExpandedElement, RsVisible, RsDocAndAttributeOwner {
    boolean isAbstract();

    Icon getIcon(int flags, boolean allowNameResolution);

    default RsAbstractableOwner getOwner() {
        return RsAbstractableUtil.getOwner(this);
    }

    default RsAbstractableOwner getOwnerBySyntaxOnly() {
        return RsAbstractableUtil.getOwnerBySyntaxOnly(this);
    }

    default RsAbstractable getSuperItem() {
        return RsAbstractableUtil.getSuperItem(this);
    }

    default java.util.List<RsAbstractable> searchForImplementations() {
        return RsAbstractableUtil.searchForImplementations(this);
    }
}
