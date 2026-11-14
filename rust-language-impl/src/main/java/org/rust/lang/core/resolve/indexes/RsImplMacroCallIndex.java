/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.indexes;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.stub.IndexSink;
import consulo.language.psi.stub.StringStubIndexExtension;
import consulo.language.psi.stub.StubElement;
import consulo.language.psi.stub.StubIndexKey;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsMacroCall;
import org.rust.lang.core.stubs.RsFileStub;
import org.rust.lang.core.stubs.RsMacroCallStub;
import org.rust.lang.core.stubs.RsModItemStub;

/**
 * The macro calls written among the items of a module - the only ones whose expansion can add an impl - by the name of
 * the called macro ({@link RsImplMacroKeys#callOf}), by the identifiers of their arguments, by the self types of the
 * impls written in them, and by {@link RsImplMacroKeys#INCLUDE} when they include a file.
 */
@ExtensionImpl
public class RsImplMacroCallIndex extends StringStubIndexExtension<RsMacroCall> {
    static final StubIndexKey<String, RsMacroCall> KEY =
        StubIndexKey.createIndexKey("org.rust.lang.core.resolve.indexes.RsImplMacroCallIndex");

    @Override
    public int getVersion() {
        return RsFileStub.Type.getStubVersion();
    }

    @Nonnull
    @Override
    public StubIndexKey<String, RsMacroCall> getKey() {
        return KEY;
    }

    public static void index(@Nonnull RsMacroCallStub stub, @Nonnull IndexSink sink) {
        StubElement<?> parent = stub.getParentStub();
        if (!(parent instanceof RsFileStub) && !(parent instanceof RsModItemStub)) return;
        String name = stub.getPath().getReferenceName();
        if (name == null) return;
        sink.occurrence(KEY, RsImplMacroKeys.callOf(name));
        String body = stub.getMacroBody();
        if (name.equals("include")) sink.occurrence(KEY, RsImplMacroKeys.INCLUDE);
        if (body == null) return;
        if (RsImplMacroKeys.includesFile(body)) sink.occurrence(KEY, RsImplMacroKeys.INCLUDE);
        for (String identifier : RsImplMacroKeys.identifiers(body)) {
            sink.occurrence(KEY, RsImplMacroKeys.argument(identifier));
        }
        for (String key : RsImplMacroKeys.implSelfTypes(body)) {
            sink.occurrence(KEY, key);
        }
    }
}
