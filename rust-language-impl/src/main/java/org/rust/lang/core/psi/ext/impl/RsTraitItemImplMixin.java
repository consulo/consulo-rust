/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.content.scope.SearchScope;
import consulo.language.psi.stub.IStubElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.icons.RsIcons;
import org.rust.lang.core.macros.RsExpandedElement;
import org.rust.lang.core.psi.impl.RsPsiImplUtil;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.RsTypeAlias;
import org.rust.lang.core.stubs.RsTraitItemStub;
import org.rust.lang.core.types.BoundElement;
import org.rust.lang.core.types.RsPsiTypeImplUtil;
import org.rust.lang.core.types.ty.Ty;

import javax.swing.*;
import java.util.Collection;
import org.rust.lang.core.psi.ext.impl.RsVisibilityUtil;
import consulo.ui.image.Image;
import org.rust.lang.core.psi.ext.*;

public abstract class RsTraitItemImplMixin extends RsStubbedNamedElementImpl<RsTraitItemStub> implements RsTraitItem {

    public RsTraitItemImplMixin(@Nonnull ASTNode node) {
        super(node);
    }

    public RsTraitItemImplMixin(@Nonnull RsTraitItemStub stub, @Nonnull IStubElementType nodeType) {
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
    public BoundElement<RsTraitItem> getImplementedTrait() {
        return new BoundElement<>(this);
    }

    @Nonnull
    @Override
    public Collection<RsTypeAlias> getAssociatedTypesTransitively() {
        return RsTraitItemImplUtil.getAssociatedTypesTransitively(this);
    }

    @Override
    public boolean isUnsafe() {
        RsTraitItemStub stub = getGreenStub();
        return stub != null ? stub.isUnsafe() : getUnsafe() != null;
    }

    @Nonnull
    @Override
    public Ty getDeclaredType() {
        return RsPsiTypeImplUtil.declaredType(this);
    }

    @Nullable
    @Override
    public PsiElement getContext() {
        return RsExpandedElement.getContextImpl(this);
    }

    @Nonnull
    @Override
    public SearchScope getUseScope() {
        SearchScope scope = RsPsiImplUtil.getDeclarationUseScope(this);
        return scope != null ? scope : super.getUseScope();
    }
}
