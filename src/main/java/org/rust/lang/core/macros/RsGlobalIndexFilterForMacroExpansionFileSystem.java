/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import com.intellij.find.ngrams.TrigramIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.impl.cache.impl.id.IdIndex;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.util.indexing.GlobalIndexFilter;
import com.intellij.util.indexing.IndexId;
import org.jetbrains.annotations.NotNull;

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
    public boolean isExcludedFromIndex(@NotNull VirtualFile virtualFile, @NotNull IndexId<?, ?> indexId) {
        return myDisabledIndices.contains(indexId) && virtualFile.getFileSystem() instanceof MacroExpansionFileSystem;
    }

    @Override
    public boolean affectsIndex(@NotNull IndexId<?, ?> indexId) {
        return false;
    }

    @Override
    public int getVersion() {
        return 0;
    }
}
