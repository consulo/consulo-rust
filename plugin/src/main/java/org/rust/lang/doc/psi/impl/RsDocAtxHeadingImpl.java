/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi.impl;

import consulo.language.ast.IElementType;
import jakarta.annotation.Nonnull;
import org.rust.lang.doc.psi.RsDocAtxHeading;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.RsModExtUtil;

public class RsDocAtxHeadingImpl extends RsDocElementImpl implements RsDocAtxHeading {

    public RsDocAtxHeadingImpl(@Nonnull IElementType type) {
        super(type);
    }

    @Nonnull
    @Override
    public RsMod getContainingMod() {
        return RsModExtUtil.getContainingMod(this);
    }
}
