/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.lang.ASTNode;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsConstParameter;
import org.rust.lang.core.psi.RsPsiImplUtil;
import org.rust.lang.core.stubs.RsConstParameterStub;

public abstract class RsConstParameterImplMixin extends RsStubbedNamedElementImpl<RsConstParameterStub> implements RsConstParameter {

    public RsConstParameterImplMixin(@NotNull ASTNode node) {
        super(node);
    }

    public RsConstParameterImplMixin(@NotNull RsConstParameterStub stub, @NotNull IStubElementType<?, ?> nodeType) {
        super(stub, nodeType);
    }

    @NotNull
    @Override
    public SearchScope getUseScope() {
        SearchScope scope = RsPsiImplUtil.getParameterUseScope(this);
        return scope != null ? scope : super.getUseScope();
    }
}
