/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs;

import consulo.language.psi.stub.IStubElementType;
import consulo.language.psi.stub.StubElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.rust.lang.core.psi.ext.RsElement;
import org.rust.stdext.HashCode;

public abstract class RsAttrProcMacroOwnerStubBase<T extends RsElement> extends RsAttributeOwnerStubBase<T>
    implements RsAttrProcMacroOwnerStub {

    protected RsAttrProcMacroOwnerStubBase(@Nullable StubElement parent, @Nonnull IStubElementType elementType) {
        super(parent, elementType);
    }

    @Nullable
    @Override
    public String getStubbedText() {
        RsProcMacroStubInfo info = getProcMacroInfo();
        return info != null ? info.getStubbedText() : null;
    }

    @Nullable
    @Override
    public HashCode getStubbedTextHash() {
        RsProcMacroStubInfo info = getProcMacroInfo();
        return info != null ? info.getStubbedTextHash() : null;
    }

    @Override
    public int getEndOfAttrsOffset() {
        RsProcMacroStubInfo info = getProcMacroInfo();
        return info != null ? info.getEndOfAttrsOffset() : 0;
    }

    @Override
    public int getStartOffset() {
        RsProcMacroStubInfo info = getProcMacroInfo();
        return info != null ? info.getStartOffset() : 0;
    }

    
    @Nullable
    public abstract RsProcMacroStubInfo getProcMacroInfo();
}
