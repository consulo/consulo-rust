/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.rust.lang.core.psi.ext.RsAbstractableOwner;

import org.rust.lang.core.macros.RsExpandedElement;
import consulo.ui.image.Image;

public interface RsAbstractable extends RsNameIdentifierOwner, RsExpandedElement, RsVisible, RsDocAndAttributeOwner {
    boolean isAbstract();

    Image getIcon(int flags, boolean allowNameResolution);

    default RsAbstractableOwner getOwner() {
        return RsPsiSupport.getInstance().owner(this);
    }

    default RsAbstractableOwner getOwnerBySyntaxOnly() {
        return RsPsiSupport.getInstance().ownerBySyntaxOnly(this);
    }

    default RsAbstractable getSuperItem() {
        return RsPsiSupport.getInstance().superItem(this);
    }

    default java.util.List<RsAbstractable> searchForImplementations() {
        return RsPsiSupport.getInstance().searchForImplementations(this);
    }
}
