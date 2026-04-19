/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi.impl;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.doc.psi.RsDocInlineLink;
import org.rust.lang.doc.psi.RsDocLinkDestination;
import org.rust.lang.doc.psi.RsDocLinkText;

public class RsDocInlineLinkImpl extends RsDocElementImpl implements RsDocInlineLink {

    public RsDocInlineLinkImpl(@NotNull IElementType type) {
        super(type);
    }

    @Override
    @NotNull
    public RsDocLinkText getLinkText() {
        return notNullChild(PsiTreeUtil.getChildOfType(this, RsDocLinkText.class));
    }

    @Override
    @NotNull
    public RsDocLinkDestination getLinkDestination() {
        return notNullChild(PsiTreeUtil.getChildOfType(this, RsDocLinkDestination.class));
    }
}
