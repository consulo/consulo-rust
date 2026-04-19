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
import org.rust.lang.core.macros.RsExpandedElement;
import org.rust.lang.core.psi.RsUseItem;
import org.rust.lang.core.stubs.RsUseItemStub;

public abstract class RsUseItemImplMixin extends RsStubbedElementImpl<RsUseItemStub> implements RsUseItem {

    public RsUseItemImplMixin(@NotNull ASTNode node) {
        super(node);
    }

    public RsUseItemImplMixin(@NotNull RsUseItemStub stub, @NotNull IStubElementType<?, ?> nodeType) {
        super(stub, nodeType);
    }

    @Nullable
    @Override
    public PsiElement getContext() {
        return RsExpandedElement.getContextImpl(this);
    }
}
