/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring;

import org.rust.lang.core.psi.RsOuterAttr;
import org.rust.lang.core.psi.ext.RsOuterAttributeOwner;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class ExtractSubsetUtils {
    private ExtractSubsetUtils() {
    }

    public static List<RsOuterAttr> findTransitiveAttributes(RsOuterAttributeOwner element, Set<String> supportedAttributes) {
        List<RsOuterAttr> result = new ArrayList<>();
        for (RsOuterAttr attr : element.getOuterAttrList()) {
            if (attr.getMetaItem() != null && supportedAttributes.contains(attr.getMetaItem().getName())) {
                result.add(attr);
            }
        }
        return result;
    }
}
