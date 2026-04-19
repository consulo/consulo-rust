/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiFile;
import com.intellij.psi.StubBuilder;
import com.intellij.psi.impl.source.tree.TreeUtil;
import com.intellij.psi.stubs.*;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IStubFileElementType;
import com.intellij.util.BitUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.parser.RustParserDefinition;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsInnerAttributeOwnerRegistry;

import java.io.IOException;
import java.util.stream.Stream;

import static org.rust.lang.core.psi.BuiltinAttributes.RS_BUILTIN_ATTRIBUTES_VERSION;
import static org.rust.lang.core.psi.RsElementTypes.*;
import static org.rust.lang.core.stubs.RsAttributeOwnerStub.CommonStubAttrFlags.*;
import static org.rust.lang.core.stubs.RsAttributeOwnerStub.FileStubAttrFlags.*;
import static org.rust.lang.core.stubs.RsAttributeOwnerStub.ModStubAttrFlags.*;

public class RsFileStub extends PsiFileStubImpl<RsFile> implements RsAttributeOwnerStub {
    private final int flags;

    public RsFileStub(@Nullable RsFile file, int flags) {
        super(file);
        this.flags = flags;
    }

    public boolean getMayHaveStdlibAttributes() {
        return BitUtil.isSet(flags, MAY_HAVE_STDLIB_ATTRIBUTES);
    }

    public boolean getMayHaveRecursionLimitAttribute() {
        return BitUtil.isSet(flags, MAY_HAVE_RECURSION_LIMIT);
    }

    @NotNull
    @Override
    public Stream<RsMetaItemStub> getRawMetaItems() {
        return RsInnerAttributeOwnerRegistry.rawMetaItemsStub(this);
    }

    @Override
    public boolean getHasAttrs() {
        return BitUtil.isSet(flags, HAS_ATTRS);
    }

    @Override
    public boolean getMayHaveCfg() {
        return BitUtil.isSet(flags, MAY_HAVE_CFG);
    }

    @Override
    public boolean getHasCfgAttr() {
        return BitUtil.isSet(flags, HAS_CFG_ATTR);
    }

    public boolean getMayHaveMacroUse() {
        return BitUtil.isSet(flags, MAY_HAVE_MACRO_USE);
    }

    @Override
    public boolean getMayHaveCustomDerive() {
        return false;
    }

    @Override
    public boolean getMayHaveCustomAttrs() {
        return false;
    }

    @NotNull
    @Override
    public IStubFileElementType<?> getType() {
        return Type;
    }

    public static final IStubFileElementType<RsFileStub> Type = new IStubFileElementType<RsFileStub>(RsLanguage.INSTANCE) {
        private static final int STUB_VERSION = 234;

        @Override
        public int getStubVersion() {
            return RustParserDefinition.PARSER_VERSION + RS_BUILTIN_ATTRIBUTES_VERSION + STUB_VERSION;
        }

        @NotNull
        @Override
        public StubBuilder getBuilder() {
            return new DefaultStubBuilder() {
                @NotNull
                @Override
                protected StubElement<?> createStubForFile(@NotNull PsiFile file) {
                    TreeUtil.ensureParsed(file.getNode());

                    if (file instanceof RsReplCodeFragment) {
                        return new RsFileStub(null, 0);
                    }

                    if (!(file instanceof RsFile)) {
                        throw new IllegalStateException("Expected RsFile");
                    }
                    RsFile rsFile = (RsFile) file;
                    int flags = RsAttributeOwnerStub.extractFlags(rsFile, new RsAttributeOwnerStub.FileStubAttrFlags());
                    return new RsFileStub(rsFile, flags);
                }

                @Override
                public boolean skipChildProcessingWhenBuildingStubs(@NotNull ASTNode parent, @NotNull ASTNode child) {
                    IElementType elementType = child.getElementType();
                    return elementType == MACRO_ARGUMENT || elementType == MACRO_BODY
                        || org.rust.lang.core.psi.RsTokenType.RS_DOC_COMMENTS.contains(elementType)
                        || (elementType == BLOCK && parent.getElementType() == FUNCTION
                            && !BlockMayHaveStubsHeuristic.getAndClearCached(child));
                }
            };
        }

        @Override
        public void serialize(@NotNull RsFileStub stub, @NotNull StubOutputStream dataStream) throws IOException {
            dataStream.writeByte(stub.flags);
        }

        @NotNull
        @Override
        public RsFileStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
            return new RsFileStub(null, dataStream.readUnsignedByte());
        }

        @NotNull
        @Override
        public String getExternalId() {
            return "Rust.file";
        }
    };
}
