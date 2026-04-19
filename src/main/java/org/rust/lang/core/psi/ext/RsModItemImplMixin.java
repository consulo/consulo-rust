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
import org.rust.lang.core.psi.RsModItem;
import org.rust.lang.core.psi.RsPsiImplUtil;
import org.rust.lang.core.stubs.RsModItemStub;

import javax.swing.*;
import org.rust.lang.core.psi.ext.RsVisibilityUtil;

public abstract class RsModItemImplMixin extends RsStubbedNamedElementImpl<RsModItemStub> implements RsModItem {

    public RsModItemImplMixin(@NotNull ASTNode node) {
        super(node);
    }

    public RsModItemImplMixin(@NotNull RsModItemStub stub, @NotNull IStubElementType<?, ?> nodeType) {
        super(stub, nodeType);
    }

    @Override
    public Icon getIcon(int flags) {
        return RsVisibilityUtil.iconWithVisibility(this, flags, RsIcons.MODULE);
    }

    @NotNull
    @Override
    public RsMod getSuper() {
        return getContainingMod();
    }

    @Nullable
    @Override
    public String getModName() {
        return getName();
    }

    @Nullable
    @Override
    public String getPathAttribute() {
        return RsDocAndAttributeOwnerUtil.getQueryAttributes(this).lookupStringValueForKey("path");
    }

    @Nullable
    @Override
    public String getCrateRelativePath() {
        return RsPsiImplUtil.modCrateRelativePath(this);
    }

    @Override
    public boolean getOwnsDirectory() {
        return true;
    }

    @Override
    public boolean isCrateRoot() {
        return false;
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
