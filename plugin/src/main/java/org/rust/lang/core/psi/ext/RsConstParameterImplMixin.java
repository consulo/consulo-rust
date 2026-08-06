/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.ast.ASTNode;
import consulo.content.scope.SearchScope;
import consulo.language.psi.stub.IStubElementType;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsConstParameter;
import org.rust.lang.core.psi.RsPsiImplUtil;
import org.rust.lang.core.stubs.RsConstParameterStub;

public abstract class RsConstParameterImplMixin extends RsStubbedNamedElementImpl<RsConstParameterStub> implements RsConstParameter {

    public RsConstParameterImplMixin(@Nonnull ASTNode node) {
        super(node);
    }

    public RsConstParameterImplMixin(@Nonnull RsConstParameterStub stub, @Nonnull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    @Nonnull
    @Override
    public SearchScope getUseScope() {
        SearchScope scope = RsPsiImplUtil.getParameterUseScope(this);
        return scope != null ? scope : super.getUseScope();
    }
}
