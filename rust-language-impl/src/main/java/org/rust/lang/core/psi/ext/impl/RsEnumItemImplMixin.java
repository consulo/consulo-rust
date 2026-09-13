/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.content.scope.SearchScope;
import consulo.language.psi.stub.IStubElementType;
import org.rust.icons.RsIcons;
import org.rust.lang.core.macros.RsExpandedElement;
import org.rust.lang.core.psi.RsEnumItem;
import org.rust.lang.core.psi.impl.RsPsiImplUtil;
import org.rust.lang.core.stubs.RsEnumItemStub;
import org.rust.lang.core.types.RsPsiTypeImplUtil;
import org.rust.lang.core.types.ty.Ty;

import javax.swing.*;
import org.rust.lang.core.psi.ext.impl.RsVisibilityUtil;
import consulo.ui.image.Image;
import org.rust.lang.core.psi.ext.*;

public abstract class RsEnumItemImplMixin extends RsStubbedNamedElementImpl<RsEnumItemStub> implements RsEnumItem {

    public RsEnumItemImplMixin(ASTNode node) {
        super(node);
    }

    public RsEnumItemImplMixin(RsEnumItemStub stub, IStubElementType nodeType) {
        super(stub, nodeType);
    }

    public consulo.ui.image.Image getIcon(int flags) {
        return RsVisibilityUtil.iconWithVisibility(this, flags, RsIcons.ENUM);
    }

    @Override
    public String getCrateRelativePath() {
        return RsPsiImplUtil.crateRelativePath(this);
    }

    @Override
    public Ty getDeclaredType() {
        return RsPsiTypeImplUtil.declaredType(this);
    }

    @Override
    public PsiElement getContext() {
        return RsExpandedElement.getContextImpl(this);
    }

    @Override
    public SearchScope getUseScope() {
        SearchScope scope = RsPsiImplUtil.getDeclarationUseScope(this);
        return scope != null ? scope : super.getUseScope();
    }
}
