/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.macros.RsExpandedElement;
import org.rust.lang.core.psi.RsForeignModItem;
import org.rust.lang.core.stubs.RsForeignModStub;

public abstract class RsForeignModItemImplMixin extends RsStubbedElementImpl<RsForeignModStub>
    implements RsForeignModItem {

    public RsForeignModItemImplMixin(@NotNull ASTNode node) {
        super(node);
    }

    public RsForeignModItemImplMixin(@NotNull RsForeignModStub stub, @NotNull IStubElementType<?, ?> elementType) {
        super(stub, elementType);
    }

    @NotNull
    @Override
    public RsVisibility getVisibility() {
        return RsVisibility.Private.INSTANCE;
    }

    @Override
    public PsiElement getContext() {
        return RsExpandedElement.getContextImpl(this);
    }
}
