/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import consulo.language.ast.ASTNode;
import consulo.content.scope.SearchScope;
import consulo.language.psi.stub.IStubElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.icons.RsIcons;
import org.rust.lang.core.psi.RsEnumVariant;
import org.rust.lang.core.psi.RsNamedFieldDecl;
import org.rust.lang.core.psi.impl.RsPsiImplUtil;
import org.rust.lang.core.stubs.RsNamedFieldDeclStub;

import javax.swing.*;
import org.rust.lang.core.psi.ext.impl.RsVisibilityUtil;
import consulo.ui.image.Image;
import org.rust.lang.core.psi.ext.*;

public abstract class RsNamedFieldDeclImplMixin extends RsStubbedNamedElementImpl<RsNamedFieldDeclStub> implements RsNamedFieldDecl {

    public RsNamedFieldDeclImplMixin(@Nonnull ASTNode node) {
        super(node);
    }

    public RsNamedFieldDeclImplMixin(@Nonnull RsNamedFieldDeclStub stub, @Nonnull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    public consulo.ui.image.Image getIcon(int flags) {
        RsFieldsOwner owner = RsFieldDeclUtil.getOwner(this);
        return owner instanceof RsEnumVariant ? RsIcons.FIELD : RsVisibilityUtil.iconWithVisibility(this, flags, RsIcons.FIELD);
    }

    @Nullable
    @Override
    public String getCrateRelativePath() {
        return RsPsiImplUtil.crateRelativePath(this);
    }

    @Nonnull
    @Override
    public SearchScope getUseScope() {
        SearchScope scope = RsPsiImplUtil.getDeclarationUseScope(this);
        return scope != null ? scope : super.getUseScope();
    }
}
