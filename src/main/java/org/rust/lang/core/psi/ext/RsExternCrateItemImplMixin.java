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
import org.rust.lang.core.psi.RsExternCrateItem;
import org.rust.lang.core.psi.RsPsiImplUtil;
import org.rust.lang.core.resolve.ref.RsExternCrateReferenceImpl;
import org.rust.lang.core.resolve.ref.RsReference;
import org.rust.lang.core.stubs.RsExternCrateItemStub;

import javax.swing.*;

public abstract class RsExternCrateItemImplMixin extends RsStubbedNamedElementImpl<RsExternCrateItemStub>
    implements RsExternCrateItem {

    public RsExternCrateItemImplMixin(@NotNull ASTNode node) {
        super(node);
    }

    public RsExternCrateItemImplMixin(@NotNull RsExternCrateItemStub stub,
                                      @NotNull IStubElementType<?, ?> elementType) {
        super(stub, elementType);
    }

    @NotNull
    @Override
    public RsReference getReference() {
        return new RsExternCrateReferenceImpl(this);
    }

    @NotNull
    @Override
    public PsiElement getReferenceNameElement() {
        PsiElement id = getIdentifier();
        if (id == null) id = getSelf();
        if (id == null) {
            throw new IllegalStateException(
                "Extern crate must contain identifier: " + this + " " + getText()
                    + " at " + getContainingFile().getVirtualFile().getPath()
            );
        }
        return id;
    }

    @NotNull
    @Override
    public String getReferenceName() {
        RsExternCrateItemStub stub = getGreenStub();
        if (stub != null) return stub.getName();
        return getReferenceNameElement().getText();
    }

    @Nullable
    @Override
    public String getName() {
        return getReferenceName();
    }

    @Nullable
    @Override
    public PsiElement getNameIdentifier() {
        return getReferenceNameElement();
    }

    @Override
    public Icon getIcon(int flags) {
        return RsIcons.CRATE;
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
