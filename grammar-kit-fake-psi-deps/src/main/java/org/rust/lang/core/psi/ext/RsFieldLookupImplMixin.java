/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.tree.IElementType;

public class RsFieldLookupImplMixin extends RsElementImpl {
    public RsFieldLookupImplMixin(IElementType type) {
        super(type);
    }
}
