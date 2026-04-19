/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.lang.ASTNode;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsPsiImplUtil;
import org.rust.lang.core.psi.RsTypeParameter;
import org.rust.lang.core.stubs.RsTypeParameterStub;
import org.rust.lang.core.types.RsPsiTypeImplUtil;
import org.rust.lang.core.types.ty.Ty;

public abstract class RsTypeParameterImplMixin extends RsStubbedNamedElementImpl<RsTypeParameterStub> implements RsTypeParameter {

    public RsTypeParameterImplMixin(@NotNull ASTNode node) {
        super(node);
    }

    public RsTypeParameterImplMixin(@NotNull RsTypeParameterStub stub, @NotNull IStubElementType<?, ?> nodeType) {
        super(stub, nodeType);
    }

    @NotNull
    @Override
    public Ty getDeclaredType() {
        return RsPsiTypeImplUtil.declaredType(this);
    }

    @NotNull
    @Override
    public SearchScope getUseScope() {
        SearchScope scope = RsPsiImplUtil.getParameterUseScope(this);
        return scope != null ? scope : super.getUseScope();
    }
}
