/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsInnerAttr;

import java.util.List;

/**
 * Outer attributes are always children of the owning node.
 * In contrast, inner attributes can be either direct
 * children or grandchildren.
 */
public interface RsInnerAttributeOwner extends RsDocAndAttributeOwner {
    @Nonnull
    default List<RsInnerAttr> getInnerAttrList() {
        return RsInnerAttributeOwnerRegistry.innerAttrs(this);
    }
}
