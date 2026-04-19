/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.ext.RsAttr;
import org.rust.lang.core.psi.ext.RsAttrUtil;

public class RemoveAttrFix extends RemoveElementFix {

    public RemoveAttrFix(@NotNull RsAttr attr) {
        super(attr, "attribute" + (RsAttrUtil.getName(attr.getMetaItem()) != null
            ? " `" + RsAttrUtil.getName(attr.getMetaItem()) + "`"
            : ""));
    }
}
