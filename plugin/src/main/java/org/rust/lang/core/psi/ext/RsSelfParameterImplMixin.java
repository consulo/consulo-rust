/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.language.psi.stub.IStubElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.ide.icons.RsIcons;
import org.rust.lang.core.psi.RsSelfParameter;
import org.rust.lang.core.stubs.RsSelfParameterStub;

import javax.swing.*;
import consulo.ui.image.Image;

public abstract class RsSelfParameterImplMixin extends RsStubbedElementImpl<RsSelfParameterStub> implements RsSelfParameter {

    public RsSelfParameterImplMixin(@Nonnull ASTNode node) {
        super(node);
    }

    public RsSelfParameterImplMixin(@Nonnull RsSelfParameterStub stub, @Nonnull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    @Nonnull
    @Override
    public PsiElement getNameIdentifier() {
        return getSelf();
    }

    @Nonnull
    @Override
    public String getName() {
        return "self";
    }

    @Override
    public PsiElement setName(@Nonnull String name) {
        // can't rename self
        throw new UnsupportedOperationException();
    }

    @Override
    public int getTextOffset() {
        return getNameIdentifier().getTextOffset();
    }

    public consulo.ui.image.Image getIcon(int flags) {
        return RsIcons.ARGUMENT;
    }
}
