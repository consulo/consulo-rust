/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.generated;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFileManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.RsPsiManager;
import org.rust.lang.core.resolve2.DefMapService;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.rust.lang.core.crate.CrateGraphService;

/**
 * The code {@link RsGeneratedIncludeProvider}s generate, per crate and file, kept until its sources change.
 * <p>
 * The def map of a crate holds the items of the code, so when regenerating a file gives other code, the def maps of
 * the crates that include it are rebuilt and the Rust structure is marked as changed.
 */
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public final class RsGeneratedIncludes {
    private record Key(int crateId, @Nonnull String fileName) {
    }

    /** Generated code, or {@code null} when no provider generates the file, with what it was generated from. */
    private record Entry(@Nullable RsGeneratedCode code, long[] sourceStamps, long vfsStamp) {
    }

    @Nonnull
    private final Project project;
    private final Map<Key, Entry> entries = new ConcurrentHashMap<>();

    @Inject
    public RsGeneratedIncludes(@Nonnull Project project) {
        this.project = project;
    }

    @Nonnull
    public static RsGeneratedIncludes getInstance(@Nonnull Project project) {
        return project.getInstance(RsGeneratedIncludes.class);
    }

    /**
     * The code of {@code fileName} - a path relative to {@code OUT_DIR} - of {@code crate}, or {@code null} when no
     * provider generates it and the file has to be read from the output directory of the build script.
     */
    @Nullable
    public RsGeneratedCode find(@Nonnull Crate crate, @Nonnull String fileName) {
        Integer crateId = crate.getId();
        if (crateId == null) return generate(crate, fileName);
        Key key = new Key(crateId, fileName);
        Entry entry = entries.get(key);
        if (entry != null && isUpToDate(entry)) return entry.code();
        RsGeneratedCode code = generate(crate, fileName);
        entries.put(key, newEntry(code));
        return code;
    }

    @Nullable
    private RsGeneratedCode generate(@Nonnull Crate crate, @Nonnull String fileName) {
        for (RsGeneratedIncludeProvider provider : project.getExtensionList(RsGeneratedIncludeProvider.class)) {
            RsGeneratedCode code = provider.generate(crate, fileName);
            if (code != null) return code;
        }
        return null;
    }

    /**
     * Regenerates the code whose sources changed; when any file comes out different, the def maps of the crates
     * that include generated code are rebuilt. Called after every physical PSI change.
     */
    public void checkSources() {
        if (entries.isEmpty()) return;
        boolean changed = false;
        for (Map.Entry<Key, Entry> mapEntry : entries.entrySet()) {
            Entry entry = mapEntry.getValue();
            if (isUpToDate(entry)) continue;
            Crate crate = findCrate(mapEntry.getKey().crateId());
            if (crate == null) {
                entries.remove(mapEntry.getKey());
                continue;
            }
            RsGeneratedCode code = generate(crate, mapEntry.getKey().fileName());
            entries.put(mapEntry.getKey(), newEntry(code));
            String oldText = entry.code() != null ? entry.code().getText() : null;
            String newText = code != null ? code.getText() : null;
            if (!Objects.equals(oldText, newText)) changed = true;
        }
        if (!changed) return;
        DefMapService defMapService = project.getInstance(DefMapService.class);
        for (Key key : entries.keySet()) {
            if (defMapService.hasDefMapFor(key.crateId())) {
                defMapService.getDefMapHolder(key.crateId()).setShouldRebuild(true);
            }
        }
        project.getInstance(RsPsiManager.class).incRustStructureModificationCount();
    }

    @Nullable
    private Crate findCrate(int crateId) {
        for (Crate crate : project.getInstance(CrateGraphService.class).getTopSortedCrates()) {
            if (Objects.equals(crate.getId(), crateId)) return crate;
        }
        return null;
    }

    @Nonnull
    private static Entry newEntry(@Nullable RsGeneratedCode code) {
        List<PsiFile> sources = code != null ? code.getSources() : List.of();
        long[] stamps = new long[sources.size()];
        for (int i = 0; i < stamps.length; i++) {
            stamps[i] = sources.get(i).getModificationStamp();
        }
        return new Entry(code, stamps, VirtualFileManager.VFS_STRUCTURE_MODIFICATIONS.getModificationCount());
    }

    /** Sources unchanged, and no file created or deleted that could add a source. */
    private static boolean isUpToDate(@Nonnull Entry entry) {
        if (entry.vfsStamp() != VirtualFileManager.VFS_STRUCTURE_MODIFICATIONS.getModificationCount()) return false;
        if (entry.code() == null) return true;
        List<PsiFile> sources = entry.code().getSources();
        for (int i = 0; i < sources.size(); i++) {
            PsiFile source = sources.get(i);
            if (!source.isValid() || source.getModificationStamp() != entry.sourceStamps()[i]) return false;
        }
        return true;
    }
}
