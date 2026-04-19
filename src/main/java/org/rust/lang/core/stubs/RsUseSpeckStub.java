/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs;

import com.intellij.psi.stubs.*;
import com.intellij.util.BitUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsUseGroup;
import org.rust.lang.core.psi.RsUseSpeck;
import org.rust.lang.core.psi.RsStubElementTypes;
import org.rust.lang.core.psi.impl.RsUseSpeckImpl;
import org.rust.stdext.BitFlagsBuilder;

import java.io.IOException;
import org.rust.lang.core.psi.ext.RsUseSpeckUtil;

public class RsUseSpeckStub extends RsElementStub<RsUseSpeck> {
    private final boolean isStarImport;
    private final boolean hasColonColon;

    private static final int IS_STAR_IMPORT_MASK = 1;
    private static final int HAS_COLON_COLON_MASK = 2;

    public RsUseSpeckStub(@Nullable StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType,
                           boolean isStarImport, boolean hasColonColon) {
        super(parent, elementType);
        this.isStarImport = isStarImport;
        this.hasColonColon = hasColonColon;
    }

    public boolean isStarImport() { return isStarImport; }
    public boolean isHasColonColon() { return hasColonColon; }

    @Nullable public RsPathStub getPath() { return (RsPathStub) findChildStubByType(RsPathStub.Type); }
    @Nullable public RsAliasStub getAlias() { return (RsAliasStub) findChildStubByType(RsAliasStub.Type); }
    @Nullable @SuppressWarnings("unchecked")
    public StubElement<RsUseGroup> getUseGroup() { return (StubElement<RsUseGroup>) findChildStubByType(RsStubElementTypes.USE_GROUP); }

    public static final RsStubElementType<RsUseSpeckStub, RsUseSpeck> Type =
        new RsStubElementType<RsUseSpeckStub, RsUseSpeck>("USE_SPECK") {
            @NotNull @Override
            public RsUseSpeckStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
                int flags = dataStream.readUnsignedByte();
                return new RsUseSpeckStub(parentStub, this,
                    BitUtil.isSet(flags, IS_STAR_IMPORT_MASK),
                    BitUtil.isSet(flags, HAS_COLON_COLON_MASK));
            }

            @Override
            public void serialize(@NotNull RsUseSpeckStub stub, @NotNull StubOutputStream dataStream) throws IOException {
                int flags = 0;
                flags = BitUtil.set(flags, IS_STAR_IMPORT_MASK, stub.isStarImport);
                flags = BitUtil.set(flags, HAS_COLON_COLON_MASK, stub.hasColonColon);
                dataStream.writeByte(flags);
            }

            @NotNull @Override
            public RsUseSpeck createPsi(@NotNull RsUseSpeckStub stub) { return new RsUseSpeckImpl(stub, this); }

            @NotNull @Override
            public RsUseSpeckStub createStub(@NotNull RsUseSpeck psi, @Nullable StubElement<?> parentStub) {
                return new RsUseSpeckStub(parentStub, this, RsUseSpeckUtil.isStarImport(psi), RsUseSpeckUtil.getHasColonColon(psi));
            }
        };
}
