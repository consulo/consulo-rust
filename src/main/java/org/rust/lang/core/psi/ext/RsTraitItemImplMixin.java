/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.icons.RsIcons;
import org.rust.lang.core.macros.RsExpandedElement;
import org.rust.lang.core.psi.RsPsiImplUtil;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.RsTypeAlias;
import org.rust.lang.core.stubs.RsTraitItemStub;
import org.rust.lang.core.types.BoundElement;
import org.rust.lang.core.types.RsPsiTypeImplUtil;
import org.rust.lang.core.types.ty.Ty;

import javax.swing.*;
import java.util.Collection;
import org.rust.lang.core.psi.ext.RsVisibilityUtil;

public abstract class RsTraitItemImplMixin extends RsStubbedNamedElementImpl<RsTraitItemStub> implements RsTraitItem {

    public RsTraitItemImplMixin(@NotNull ASTNode node) {
        super(node);
    }

    public RsTraitItemImplMixin(@NotNull RsTraitItemStub stub, @NotNull IStubElementType<?, ?> nodeType) {
        super(stub, nodeType);
    }

    @Override
    public Icon getIcon(int flags) {
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

    @NotNull
    @Override
    public Collection<RsTypeAlias> getAssociatedTypesTransitively() {
        return RsTraitItemImplUtil.getAssociatedTypesTransitively(this);
    }

    @Override
    public boolean isUnsafe() {
        RsTraitItemStub stub = getGreenStub();
        return stub != null ? stub.isUnsafe() : getUnsafe() != null;
    }

    @NotNull
    @Override
    public Ty getDeclaredType() {
        return RsPsiTypeImplUtil.declaredType(this);
    }

    @Nullable
    @Override
    public PsiElement getContext() {
        return RsExpandedElement.getContextImpl(this);
    }

    @NotNull
    @Override
    public SearchScope getUseScope() {
        SearchScope scope = RsPsiImplUtil.getDeclarationUseScope(this);
        return scope != null ? scope : super.getUseScope();
    }
}
