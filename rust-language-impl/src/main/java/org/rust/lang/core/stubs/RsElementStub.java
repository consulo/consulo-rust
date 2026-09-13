/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs;

import consulo.language.psi.stub.IStubElementType;
import consulo.language.psi.stub.StubBase;
import consulo.language.psi.stub.StubElement;
import org.rust.lang.core.psi.ext.RsElement;

public abstract class RsElementStub<PsiT extends RsElement> extends StubBase<PsiT> {
    protected RsElementStub(StubElement parent, IStubElementType<? extends StubElement, ?> elementType) {
        super(parent, elementType);
    }
}
