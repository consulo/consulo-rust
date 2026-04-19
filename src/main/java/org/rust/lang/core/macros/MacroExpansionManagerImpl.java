/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
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
import org.rust.stdext.RsResult;

/**
 * The main implementation of {@link MacroExpansionManager}.
 * <p>
 * This is a state-component that persists directory names. The actual macro expansion
 * <p>
 * Note: The full implementation of this class involves many private inner classes and heavy
 * integration with DefMap, PsiManager, CargoProjects, etc. This Java conversion provides
 */
@State(name = "MacroExpansionManager", storages = {
    @Storage(value = StoragePathMacros.WORKSPACE_FILE, roamingType = RoamingType.DISABLED),
    @Storage(value = "misc.xml", roamingType = RoamingType.DISABLED, deprecated = true)
})
public class MacroExpansionManagerImpl
    implements MacroExpansionManager,
    PersistentStateComponent<MacroExpansionManagerImpl.PersistentState>,
    Disposable {

    @NotNull
    private final Project myProject;

    private volatile boolean isDisposed = false;

    public MacroExpansionManagerImpl(@NotNull Project project) {
        myProject = project;
    }

    // --- PersistentState ---

    public static class PersistentState {
        @Nullable
        public String directoryName;

        public PersistentState() {
            this(null);
        }

        public PersistentState(@Nullable String directoryName) {
            this.directoryName = directoryName;
        }
    }

    @Nullable
    @Override
    public PersistentState getState() {
        return new PersistentState();
    }

    @Override
    public void loadState(@NotNull PersistentState state) {
        // The actual loading logic delegates to MacroExpansionServiceBuilder and is highly
        // This is a structural placeholder for the Java conversion.
    }

    @Override
    public void noStateLoaded() {
        loadState(new PersistentState(null));
    }

    // --- MacroExpansionManager interface ---

    @Nullable
    @Override
    public VirtualFile getIndexableDirectory() {
        return null;
    }

    @NotNull
    @Override
    public CachedValueProvider.Result<RsResult<MacroExpansion, GetMacroExpansionError>> getExpansionFor(@NotNull RsPossibleMacroCall call) {
        return CachedValueProvider.Result.create(
            new RsResult.Err<>(GetMacroExpansionError.MacroExpansionEngineIsNotReady),
            com.intellij.openapi.util.ModificationTracker.EVER_CHANGED
        );
    }

    @Nullable
    @Override
    public RsPossibleMacroCall getExpandedFrom(@NotNull RsExpandedElement element) {
        return null;
    }

    @Nullable
    @Override
    public RsMacroCall getIncludedFrom(@NotNull RsFile file) {
        return null;
    }

    @Nullable
    @Override
    public PsiElement getContextOfMacroCallExpandedFrom(@NotNull RsFile stubParent) {
        return null;
    }

    @Override
    public boolean isExpansionFileOfCurrentProject(@NotNull VirtualFile file) {
        return false;
    }

    @Nullable
    @Override
    public Integer getCrateForExpansionFile(@NotNull VirtualFile file) {
        return null;
    }

    @Override
    public void reexpand() {
    }

    @NotNull
    @Override
    public MacroExpansionMode getMacroExpansionMode() {
        return MacroExpansionMode.OLD;
    }

    @TestOnly
    @NotNull
    @Override
    public Disposable setUnitTestExpansionModeAndDirectory(
        @NotNull MacroExpansionScope mode,
        @NotNull String cacheDirectory,
        boolean clearCacheBeforeDispose
    ) {
        return () -> {};
    }

    @TestOnly
    @Override
    public void updateInUnitTestMode() {
    }

    @TestOnly
    @Override
    public void setMacroExpansionEnabled(boolean enabled) {
    }

    @Override
    public void dispose() {
        isDisposed = true;
    }
}
