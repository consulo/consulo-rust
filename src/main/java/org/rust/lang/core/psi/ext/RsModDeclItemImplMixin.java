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
import org.rust.lang.core.psi.RsModDeclItem;
import org.rust.lang.core.psi.RsPsiImplUtil;
import org.rust.lang.core.resolve.ref.RsModReferenceImpl;
import org.rust.lang.core.resolve.ref.RsReference;
import org.rust.lang.core.stubs.RsModDeclItemStub;

import javax.swing.*;
import org.rust.lang.core.psi.ext.RsVisibilityUtil;

public abstract class RsModDeclItemImplMixin extends RsStubbedNamedElementImpl<RsModDeclItemStub> implements RsModDeclItem {

    public RsModDeclItemImplMixin(@NotNull ASTNode node) {
        super(node);
    }

    public RsModDeclItemImplMixin(@NotNull RsModDeclItemStub stub, @NotNull IStubElementType<?, ?> nodeType) {
        super(stub, nodeType);
    }

    @NotNull
    @Override
    public RsReference getReference() {
        return new RsModReferenceImpl(this);
    }

    @NotNull
    @Override
    public PsiElement getReferenceNameElement() {
        return getIdentifier();
    }

    @NotNull
    @Override
    public String getReferenceName() {
        return getName();
    }

    @Override
    public Icon getIcon(int flags) {
        return RsVisibilityUtil.iconWithVisibility(this, flags, RsIcons.MODULE);
    }

    @Nullable
    @Override
    public String getCrateRelativePath() {
        return RsPsiImplUtil.crateRelativePath(this);
    }

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
