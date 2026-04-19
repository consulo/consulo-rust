/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi.impl;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.doc.psi.RsDocLinkLabel;
import org.rust.lang.doc.psi.RsDocLinkReferenceFull;
import org.rust.lang.doc.psi.RsDocLinkText;

public class RsDocLinkReferenceFullImpl extends RsDocElementImpl implements RsDocLinkReferenceFull {

    public RsDocLinkReferenceFullImpl(@NotNull IElementType type) {
        super(type);
    }

    @Override
    @NotNull
    public RsDocLinkText getLinkText() {
        return notNullChild(PsiTreeUtil.getChildOfType(this, RsDocLinkText.class));
    }

    @Override
    @NotNull
    public RsDocLinkLabel getLinkLabel() {
        return notNullChild(PsiTreeUtil.getChildOfType(this, RsDocLinkLabel.class));
    }
}
