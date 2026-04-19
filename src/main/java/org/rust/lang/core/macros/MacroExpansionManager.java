/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.CachedValueProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.rust.lang.core.crate.CratePersistentId;
import org.rust.lang.core.macros.errors.GetMacroExpansionError;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.RsMacroCall;
import org.rust.lang.core.macros.RsExpandedElement;
import org.rust.lang.core.psi.ext.RsPossibleMacroCall;
import org.rust.openapiext.Testmark;
import org.rust.stdext.RsResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public interface MacroExpansionManager {

    @Nullable
    VirtualFile getIndexableDirectory();

    @NotNull
    CachedValueProvider.Result<RsResult<MacroExpansion, GetMacroExpansionError>> getExpansionFor(@NotNull RsPossibleMacroCall call);

    @Nullable
    RsPossibleMacroCall getExpandedFrom(@NotNull RsExpandedElement element);

    @Nullable
    RsMacroCall getIncludedFrom(@NotNull RsFile file);

    /**
     * An optimized equivalent for getExpandedFrom(macroCall)?.contextToSetForExpansion
     */
    @Nullable
    PsiElement getContextOfMacroCallExpandedFrom(@NotNull RsFile stubParent);

    boolean isExpansionFileOfCurrentProject(@NotNull VirtualFile file);

    @Nullable
    Integer getCrateForExpansionFile(@NotNull VirtualFile file);

    void reexpand();

    @NotNull
    MacroExpansionMode getMacroExpansionMode();

    @TestOnly
    @NotNull
    Disposable setUnitTestExpansionModeAndDirectory(
        @NotNull MacroExpansionScope mode,
        @NotNull String cacheDirectory,
        boolean clearCacheBeforeDispose
    );

    @TestOnly
    void updateInUnitTestMode();

    @TestOnly
    void setMacroExpansionEnabled(boolean enabled);

    static boolean isExpansionFile(@NotNull VirtualFile file) {
        return file.getFileSystem() == MacroExpansionFileSystem.getInstance();
    }

    static void invalidateCaches() {
        Path markerFile = getCorruptionMarkerFile();
        try {
            Path parent = markerFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.createFile(markerFile);
        } catch (IOException ignored) {
        }
    }

    static void checkInvalidatedStorage() {
        synchronized (MacroExpansionManager.class) {
            if (Files.exists(getCorruptionMarkerFile())) {
                try {
                    MacroExpansionManagerUtil.getBaseMacroDir().toFile().deleteOnExit();
                    // Try to clean directory
                    org.rust.stdext.PathUtil.cleanDirectory(MacroExpansionManagerUtil.getBaseMacroDir());
                } catch (IOException e) {
                    MacroExpansionManagerUtil.MACRO_LOG.warn(e);
                }
            }
        }
    }

    @NotNull
    private static Path getCorruptionMarkerFile() {
        return MacroExpansionManagerUtil.getBaseMacroDir().resolve("corruption.marker");
    }

    class Testmarks {
        public static final Testmark TooDeepExpansion = new Testmark();
    }
}
