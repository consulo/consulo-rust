/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi.impl;

import consulo.language.ast.IElementType;
import consulo.language.psi.util.PsiTreeUtil;
import jakarta.annotation.Nonnull;
import org.rust.lang.doc.psi.RsDocLinkLabel;
import org.rust.lang.doc.psi.RsDocLinkReferenceFull;
import org.rust.lang.doc.psi.RsDocLinkText;

public class RsDocLinkReferenceFullImpl extends RsDocElementImpl implements RsDocLinkReferenceFull {

    public RsDocLinkReferenceFullImpl(@Nonnull IElementType type) {
        super(type);
    }

    @Override
    @Nonnull
    public RsDocLinkText getLinkText() {
        return notNullChild(PsiTreeUtil.getChildOfType(this, RsDocLinkText.class));
    }

    @Override
    @Nonnull
    public RsDocLinkLabel getLinkLabel() {
        return notNullChild(PsiTreeUtil.getChildOfType(this, RsDocLinkLabel.class));
    }
}
