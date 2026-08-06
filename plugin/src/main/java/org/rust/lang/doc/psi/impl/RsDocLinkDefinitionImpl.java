/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi.impl;

import consulo.language.ast.IElementType;
import consulo.language.psi.util.PsiTreeUtil;
import jakarta.annotation.Nonnull;
import org.rust.lang.doc.psi.RsDocLinkDefinition;
import org.rust.lang.doc.psi.RsDocLinkDestination;
import org.rust.lang.doc.psi.RsDocLinkLabel;

public class RsDocLinkDefinitionImpl extends RsDocElementImpl implements RsDocLinkDefinition {

    public RsDocLinkDefinitionImpl(@Nonnull IElementType type) {
        super(type);
    }

    @Override
    @Nonnull
    public RsDocLinkLabel getLinkLabel() {
        return notNullChild(PsiTreeUtil.getChildOfType(this, RsDocLinkLabel.class));
    }

    @Override
    @Nonnull
    public RsDocLinkDestination getLinkDestination() {
        return notNullChild(PsiTreeUtil.getChildOfType(this, RsDocLinkDestination.class));
    }
}
