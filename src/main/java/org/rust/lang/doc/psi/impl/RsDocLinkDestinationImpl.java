/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi.impl;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.doc.psi.RsDocLinkDestination;

public class RsDocLinkDestinationImpl extends RsDocElementImpl implements RsDocLinkDestination {

    public RsDocLinkDestinationImpl(@NotNull IElementType type) {
        super(type);
    }
}
