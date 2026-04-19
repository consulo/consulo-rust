/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi.impl;

import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.doc.psi.RsDocLinkDefinition;
import org.rust.lang.doc.psi.RsDocLinkDestination;
import org.rust.lang.doc.psi.RsDocLinkLabel;

public class RsDocLinkDefinitionImpl extends RsDocElementImpl implements RsDocLinkDefinition {

    public RsDocLinkDefinitionImpl(@NotNull IElementType type) {
        super(type);
    }

    @Override
    @NotNull
    public RsDocLinkLabel getLinkLabel() {
        return notNullChild(PsiTreeUtil.getChildOfType(this, RsDocLinkLabel.class));
    }

    @Override
    @NotNull
    public RsDocLinkDestination getLinkDestination() {
        return notNullChild(PsiTreeUtil.getChildOfType(this, RsDocLinkDestination.class));
    }
}
