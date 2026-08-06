/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.ast.ASTNode;
import consulo.language.psi.stub.IStubElementType;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsUseSpeck;
import org.rust.lang.core.stubs.RsUseSpeckStub;

public abstract class RsUseSpeckImplMixin extends RsStubbedElementImpl<RsUseSpeckStub> implements RsUseSpeck {

    public RsUseSpeckImplMixin(@Nonnull ASTNode node) {
        super(node);
    }

    public RsUseSpeckImplMixin(@Nonnull RsUseSpeckStub stub, @Nonnull IStubElementType nodeType) {
        super(stub, nodeType);
    }
}
