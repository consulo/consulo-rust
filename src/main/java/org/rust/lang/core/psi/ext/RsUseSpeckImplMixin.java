/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsUseSpeck;
import org.rust.lang.core.stubs.RsUseSpeckStub;

public abstract class RsUseSpeckImplMixin extends RsStubbedElementImpl<RsUseSpeckStub> implements RsUseSpeck {

    public RsUseSpeckImplMixin(@NotNull ASTNode node) {
        super(node);
    }

    public RsUseSpeckImplMixin(@NotNull RsUseSpeckStub stub, @NotNull IStubElementType<?, ?> nodeType) {
        super(stub, nodeType);
    }
}
