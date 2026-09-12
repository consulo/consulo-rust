/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import consulo.logging.Logger;
import consulo.util.dataholder.Key;
import com.intellij.psi.stubs.PrebuiltStubsProvider;
import com.intellij.psi.stubs.SerializedStubTree;
import consulo.language.psi.stub.SerializerNotFoundException;
import consulo.language.psi.stub.Stub;
import consulo.language.psi.stub.FileContent;
import jakarta.annotation.Nullable;
import consulo.virtualFileSystem.VirtualFile;

/**
 * Used in a couple with {@link MacroExpansionSharedCache} to provide macro expansion cache shared between projects.
 * This is not a real <i>prebuilt</i> stubs provider. This extension point is used to intercept platform's
 * stub creation.
 */
@SuppressWarnings("UnstableApiUsage")
public class MacroExpansionStubsProvider implements PrebuiltStubsProvider {

    private static final Logger LOG = Logger.getInstance(MacroExpansionStubsProvider.class);
    private static final Key<SerializedStubTree> SERIALIZED_STUB_KEY =
        Key.create("org.rust.lang.core.macros.MacroExpansionStubsProvider.serializedStubKey");

    @Nullable
    public SerializedStubTree findStub(FileContent fileContent) {
        return findSerializedStubForMacroExpansionFile(fileContent);
    }

    @Override
    @Nullable
    public Stub findStub(consulo.virtualFileSystem.VirtualFile file) {
        // PrebuiltStubsProvider in Consulo only provides a VirtualFile-based lookup;
        // the FileContent-based path is used internally from findSerializedStubForMacroExpansionFile.
        return null;
    }

    @Nullable
    private static SerializedStubTree findSerializedStubForMacroExpansionFile(FileContent fileContent) {
        var file = fileContent.getFile();
        if (!MacroExpansionManager.isExpansionFile(file)) return null;

        SerializedStubTree cached = fileContent.getUserData(SERIALIZED_STUB_KEY);
        if (cached != null) return cached;

        if (!MacroExpansionSharedCache.getInstance().isEnabled()) return null;

        var hashAndVersion = ExpandedMacroStorage.extractMixHashAndMacroStorageVersion(file);
        if (hashAndVersion == null) return null;
        if (hashAndVersion.getSecond() != ExpandedMacroStorage.MACRO_STORAGE_VERSION) return null;
        var hash = hashAndVersion.getFirst();
        if (hash == null) return null;

        SerializedStubTree serializedStub = MacroExpansionSharedCache.getInstance().cachedBuildStub(fileContent, hash);
        fileContent.putUserData(SERIALIZED_STUB_KEY, serializedStub);
        return serializedStub;
    }

    @Nullable
    public static Stub findStubForMacroExpansionFile(FileContent fileContent) {
        SerializedStubTree serializedStub = findSerializedStubForMacroExpansionFile(fileContent);
        if (serializedStub == null) return null;
        return serializedStub.getStub();
    }
}
