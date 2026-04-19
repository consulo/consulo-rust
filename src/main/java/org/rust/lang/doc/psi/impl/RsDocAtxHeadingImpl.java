/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi.impl;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.doc.psi.RsDocAtxHeading;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.RsModExtUtil;

public class RsDocAtxHeadingImpl extends RsDocElementImpl implements RsDocAtxHeading {

    public RsDocAtxHeadingImpl(@NotNull IElementType type) {
        super(type);
    }

    @NotNull
    @Override
    public RsMod getContainingMod() {
        return RsModExtUtil.getContainingMod(this);
    }
}
