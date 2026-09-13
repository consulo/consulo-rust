/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.language.psi.stub.IStubElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.icons.RsIcons;
import org.rust.lang.core.macros.RsExpandedElement;
import org.rust.lang.core.psi.impl.RsPsiImplUtil;
import org.rust.lang.core.psi.RsTraitAlias;
import org.rust.lang.core.stubs.RsTraitAliasStub;

import javax.swing.*;
import org.rust.lang.core.psi.ext.impl.RsVisibilityUtil;
import consulo.ui.image.Image;
import org.rust.lang.core.psi.ext.*;

public abstract class RsTraitAliasImplMixin extends RsStubbedNamedElementImpl<RsTraitAliasStub> implements RsTraitAlias {

    public RsTraitAliasImplMixin(@Nonnull ASTNode node) {
        super(node);
    }

    public RsTraitAliasImplMixin(@Nonnull RsTraitAliasStub stub, @Nonnull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    public consulo.ui.image.Image getIcon(int flags) {
        return RsVisibilityUtil.iconWithVisibility(this, flags, RsIcons.TRAIT);
    }

    @Nullable
    @Override
    public String getCrateRelativePath() {
        return RsPsiImplUtil.crateRelativePath(this);
    }

    @Nullable
    @Override
    public PsiElement getContext() {
        return RsExpandedElement.getContextImpl(this);
    }
}
