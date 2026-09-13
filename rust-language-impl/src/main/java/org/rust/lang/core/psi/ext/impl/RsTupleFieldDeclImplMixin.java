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
import org.rust.lang.core.psi.impl.RsPsiImplUtil;
import org.rust.lang.core.psi.RsTupleFieldDecl;
import org.rust.lang.core.stubs.RsPlaceholderStub;

import javax.swing.*;
import org.rust.lang.core.psi.ext.impl.RsVisibilityUtil;
import consulo.ui.image.Image;
import org.rust.lang.core.psi.ext.*;

public abstract class RsTupleFieldDeclImplMixin extends RsStubbedElementImpl<RsPlaceholderStub<?>> implements RsTupleFieldDecl {

    public RsTupleFieldDeclImplMixin(@Nonnull ASTNode node) {
        super(node);
    }

    public RsTupleFieldDeclImplMixin(@Nonnull RsPlaceholderStub<?> stub, @Nonnull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    public consulo.ui.image.Image getIcon(int flags) {
        RsFieldsOwner owner = RsFieldDeclUtil.getOwner(this);
        if (owner instanceof RsEnumVariant) {
            return RsIcons.FIELD;
        }
        return RsVisibilityUtil.iconWithVisibility(this, flags, RsIcons.FIELD);
    }

    @Nullable
    @Override
    public String getName() {
        Integer pos = RsTupleFieldDeclUtil.getPosition(this);
        return pos != null ? pos.toString() : null;
    }

    @Nonnull
    @Override
    public SearchScope getUseScope() {
        SearchScope scope = RsPsiImplUtil.getDeclarationUseScope(this);
        return scope != null ? scope : super.getUseScope();
    }
}
