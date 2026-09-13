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
import org.rust.lang.core.psi.RsStructItem;
import org.rust.lang.core.stubs.RsStructItemStub;
import org.rust.lang.core.types.RsPsiTypeImplUtil;
import org.rust.lang.core.types.ty.Ty;

import javax.swing.*;
import org.rust.lang.core.psi.ext.impl.RsVisibilityUtil;
import consulo.ui.image.Image;
import org.rust.lang.core.psi.ext.*;

public abstract class RsStructItemImplMixin extends RsStubbedNamedElementImpl<RsStructItemStub> implements RsStructItem {

    public RsStructItemImplMixin(@Nonnull ASTNode node) {
        super(node);
    }

    public RsStructItemImplMixin(@Nonnull RsStructItemStub stub, @Nonnull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    public consulo.ui.image.Image getIcon(int flags) {
        RsStructKind kind = RsStructItemUtil.getKind(this);
        consulo.ui.image.Image baseIcon;
        switch (kind) {
            case STRUCT:
                baseIcon = RsIcons.STRUCT;
                break;
            case UNION:
                baseIcon = RsIcons.UNION;
                break;
            default:
                baseIcon = RsIcons.STRUCT;
                break;
        }
        return RsVisibilityUtil.iconWithVisibility(this, flags, baseIcon);
    }

    @Nullable
    @Override
    public String getCrateRelativePath() {
        return RsPsiImplUtil.crateRelativePath(this);
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
