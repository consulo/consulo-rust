/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import consulo.language.internal.TrigramIndex;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.internal.psi.stub.IdIndex;
import consulo.language.psi.search.FilenameIndex;
import consulo.language.index.impl.internal.GlobalIndexFilter;
import consulo.index.io.IndexId;
import jakarta.annotation.Nonnull;

import java.util.HashSet;
import java.util.Set;

/**
 * Disables some indexes for Rust macro expansions (i.e. for files in {@link MacroExpansionFileSystem})
 */
@SuppressWarnings({"UnstableApiUsage", "deprecation", "removal"})
public class RsGlobalIndexFilterForMacroExpansionFileSystem implements GlobalIndexFilter {

    /** Please, bump {@code MACRO_STORAGE_VERSION} if you change this set */
    private final Set<IndexId<?, ?>> myDisabledIndices;

    public RsGlobalIndexFilterForMacroExpansionFileSystem() {
        myDisabledIndices = new HashSet<>();
        myDisabledIndices.add(IdIndex.NAME);
        myDisabledIndices.add(TrigramIndex.INDEX_ID);
        myDisabledIndices.add(FilenameIndex.NAME);
    }

    @Override
    public boolean isExcludedFromIndex(@Nonnull VirtualFile virtualFile, @Nonnull IndexId<?, ?> indexId) {
        return myDisabledIndices.contains(indexId) && virtualFile.getFileSystem() instanceof MacroExpansionFileSystem;
    }

    @Override
    public boolean affectsIndex(@Nonnull IndexId<?, ?> indexId) {
        return false;
    }

    @Override
    public int getVersion() {
        return 0;
    }
}
