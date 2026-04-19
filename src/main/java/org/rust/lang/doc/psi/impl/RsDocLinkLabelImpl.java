/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi.impl;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.doc.psi.RsDocLinkLabel;

public class RsDocLinkLabelImpl extends RsDocElementImpl implements RsDocLinkLabel {

    public RsDocLinkLabelImpl(@NotNull IElementType type) {
        super(type);
    }
}
