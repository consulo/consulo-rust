/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import consulo.language.ast.ASTNode;
import consulo.content.scope.SearchScope;
import consulo.language.psi.stub.IStubElementType;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.impl.RsPsiImplUtil;
import org.rust.lang.core.psi.RsTypeParameter;
import org.rust.lang.core.stubs.RsTypeParameterStub;
import org.rust.lang.core.types.RsPsiTypeImplUtil;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.psi.ext.*;

public abstract class RsTypeParameterImplMixin extends RsStubbedNamedElementImpl<RsTypeParameterStub> implements RsTypeParameter {

    public RsTypeParameterImplMixin(@Nonnull ASTNode node) {
        super(node);
    }

    public RsTypeParameterImplMixin(@Nonnull RsTypeParameterStub stub, @Nonnull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    @Nonnull
    @Override
    public Ty getDeclaredType() {
        return RsPsiTypeImplUtil.declaredType(this);
    }

    @Nonnull
    @Override
    public SearchScope getUseScope() {
        SearchScope scope = RsPsiImplUtil.getParameterUseScope(this);
        return scope != null ? scope : super.getUseScope();
    }
}
