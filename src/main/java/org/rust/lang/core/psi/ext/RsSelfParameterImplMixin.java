/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.icons.RsIcons;
import org.rust.lang.core.psi.RsSelfParameter;
import org.rust.lang.core.stubs.RsSelfParameterStub;

import javax.swing.*;

public abstract class RsSelfParameterImplMixin extends RsStubbedElementImpl<RsSelfParameterStub> implements RsSelfParameter {

    public RsSelfParameterImplMixin(@NotNull ASTNode node) {
        super(node);
    }

    public RsSelfParameterImplMixin(@NotNull RsSelfParameterStub stub, @NotNull IStubElementType<?, ?> nodeType) {
        super(stub, nodeType);
    }

    @NotNull
    @Override
    public PsiElement getNameIdentifier() {
        return getSelf();
    }

    @NotNull
    @Override
    public String getName() {
        return "self";
    }

    @Override
    public PsiElement setName(@NotNull String name) {
        // can't rename self
        throw new UnsupportedOperationException();
    }

    @Override
    public int getTextOffset() {
        return getNameIdentifier().getTextOffset();
    }

    @Override
    public Icon getIcon(int flags) {
        return RsIcons.ARGUMENT;
    }
}
