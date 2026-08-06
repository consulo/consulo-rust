/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi.impl;

import consulo.language.impl.psi.LeafPsiElement;
import consulo.language.ast.IElementType;
import jakarta.annotation.Nonnull;
import org.rust.lang.doc.psi.RsDocGap;

public class RsDocGapImpl extends LeafPsiElement implements RsDocGap {

    public RsDocGapImpl(@Nonnull IElementType type, @Nonnull CharSequence text) {
        super(type, text);
    }

    @Override
    @Nonnull
    public IElementType getTokenType() {
        return getNode().getElementType();
    }
}
