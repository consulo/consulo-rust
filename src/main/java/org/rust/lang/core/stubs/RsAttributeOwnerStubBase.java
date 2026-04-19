/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs;

import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubBase;
import com.intellij.psi.stubs.StubElement;
import com.intellij.util.BitUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsInnerAttributeOwnerRegistry;

import java.util.stream.Stream;

import static org.rust.lang.core.stubs.RsAttributeOwnerStub.CommonStubAttrFlags.*;

public abstract class RsAttributeOwnerStubBase<T extends RsElement> extends StubBase<T>
    implements RsAttributeOwnerStub {

    protected RsAttributeOwnerStubBase(@Nullable StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType) {
        super(parent, elementType);
    }

    @NotNull
    @Override
    public Stream<RsMetaItemStub> getRawMetaItems() {
        return RsInnerAttributeOwnerRegistry.rawMetaItemsStub(this);
    }

    @NotNull
    @Override
    public Stream<RsMetaItemStub> getRawOuterMetaItems() {
        return RsInnerAttributeOwnerRegistry.rawOuterMetaItemsStub(this);
    }

    @Override
    public boolean getHasAttrs() {
        return BitUtil.isSet(getFlags(), HAS_ATTRS);
    }

    @Override
    public boolean getMayHaveCfg() {
        return BitUtil.isSet(getFlags(), MAY_HAVE_CFG);
    }

    @Override
    public boolean getHasCfgAttr() {
        return BitUtil.isSet(getFlags(), HAS_CFG_ATTR);
    }

    @Override
    public boolean getMayHaveCustomDerive() {
        return BitUtil.isSet(getFlags(), MAY_HAVE_CUSTOM_DERIVE);
    }

    @Override
    public boolean getMayHaveCustomAttrs() {
        return BitUtil.isSet(getFlags(), MAY_HAVE_CUSTOM_ATTRS);
    }

    protected abstract int getFlags();
}
