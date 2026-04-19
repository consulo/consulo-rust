/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi.impl;

import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.doc.psi.RsDocGap;

public class RsDocGapImpl extends LeafPsiElement implements RsDocGap {

    public RsDocGapImpl(@NotNull IElementType type, @NotNull CharSequence text) {
        super(type, text);
    }

    @Override
    @NotNull
    public IElementType getTokenType() {
        return getNode().getElementType();
    }
}
