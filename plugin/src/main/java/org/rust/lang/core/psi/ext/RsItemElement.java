/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.rust.lang.core.macros.RsExpandedElement;

/**
 *
 * Note: don't forget to add an element type to {@code org.rust.lang.core.psi.RS_ITEMS}
 * when implementing {@code RsItemElement}.
 */
public interface RsItemElement extends RsVisibilityOwner, RsOuterAttributeOwner, RsExpandedElement {
    default String getItemKindName() {
        return RsItemElementUtil.getItemKindName(this);
    }
}
