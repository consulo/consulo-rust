/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.indexes;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.stub.IndexSink;
import consulo.language.psi.stub.StringStubIndexExtension;
import consulo.language.psi.stub.StubIndexKey;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsMacro;
import org.rust.lang.core.stubs.RsFileStub;
import org.rust.lang.core.stubs.RsMacroStub;

/**
 * The {@code macro_rules!} definitions that write an impl, by the self types of those impls, and the ones that include a
 * file, by {@link RsImplMacroKeys#INCLUDE}.
 */
@ExtensionImpl
public class RsImplMacroDefIndex extends StringStubIndexExtension<RsMacro> {
    static final StubIndexKey<String, RsMacro> KEY =
        StubIndexKey.createIndexKey("org.rust.lang.core.resolve.indexes.RsImplMacroDefIndex");

    @Override
    public int getVersion() {
        return RsFileStub.Type.getStubVersion();
    }

    @Nonnull
    @Override
    public StubIndexKey<String, RsMacro> getKey() {
        return KEY;
    }

    public static void index(@Nonnull RsMacroStub stub, @Nonnull IndexSink sink) {
        String body = stub.getMacroBody();
        if (stub.getName() == null || body == null) return;
        for (String key : RsImplMacroKeys.implSelfTypes(body)) {
            sink.occurrence(KEY, key);
        }
        if (RsImplMacroKeys.includesFile(body)) sink.occurrence(KEY, RsImplMacroKeys.INCLUDE);
    }
}
