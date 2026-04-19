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
import org.rust.lang.core.psi.RsAlias;
import org.rust.lang.core.stubs.RsAliasStub;

public abstract class RsAliasImplMixin extends RsStubbedNamedElementImpl<RsAliasStub> implements RsAlias {

    public RsAliasImplMixin(@NotNull ASTNode node) {
        super(node);
    }

    public RsAliasImplMixin(@NotNull RsAliasStub stub, @NotNull IStubElementType<?, ?> elementType) {
        super(stub, elementType);
    }

    @Nullable
    @Override
    public PsiElement getNameIdentifier() {
        // `use Foo as _;` really should be unnamed, but "_" is not a valid name in rust, so I think it's ok
        PsiElement id = getIdentifier();
        if (id != null) return id;
        return getUnderscore();
    }
}
