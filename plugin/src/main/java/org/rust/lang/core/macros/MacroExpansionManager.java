/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import consulo.disposer.Disposable;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;
import consulo.application.util.CachedValueProvider;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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

    @Nonnull
    CachedValueProvider.Result<RsResult<MacroExpansion, GetMacroExpansionError>> getExpansionFor(@Nonnull RsPossibleMacroCall call);

    @Nullable
    RsPossibleMacroCall getExpandedFrom(@Nonnull RsExpandedElement element);

    @Nullable
    RsMacroCall getIncludedFrom(@Nonnull RsFile file);

    /**
     * An optimized equivalent for getExpandedFrom(macroCall)?.contextToSetForExpansion
     */
    @Nullable
    PsiElement getContextOfMacroCallExpandedFrom(@Nonnull RsFile stubParent);

    boolean isExpansionFileOfCurrentProject(@Nonnull VirtualFile file);

    @Nullable
    Integer getCrateForExpansionFile(@Nonnull VirtualFile file);

    void reexpand();

    @Nonnull
    MacroExpansionMode getMacroExpansionMode();

    
    @Nonnull
    Disposable setUnitTestExpansionModeAndDirectory(
        @Nonnull MacroExpansionScope mode,
        @Nonnull String cacheDirectory,
        boolean clearCacheBeforeDispose
    );

    
    void updateInUnitTestMode();

    
    void setMacroExpansionEnabled(boolean enabled);

    static boolean isExpansionFile(@Nonnull VirtualFile file) {
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

    @Nonnull
    private static Path getCorruptionMarkerFile() {
        return MacroExpansionManagerUtil.getBaseMacroDir().resolve("corruption.marker");
    }

    class Testmarks {
        public static final Testmark TooDeepExpansion = new Testmark();
    }
}
