/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import consulo.disposer.Disposable;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.RoamingType;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.project.Project;
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
import org.rust.stdext.RsResult;
import consulo.annotation.component.ServiceImpl;
import jakarta.inject.Inject;
import consulo.component.util.ModificationTracker;

/**
 * The main implementation of {@link MacroExpansionManager}.
 * <p>
 * This is a state-component that persists the expansion directory name.
 */
@State(name = "MacroExpansionManager", storages = {
    @Storage(value = StoragePathMacros.WORKSPACE_FILE, roamingType = RoamingType.DISABLED),
    @Storage(value = "misc.xml", roamingType = RoamingType.DISABLED, deprecated = true)
})
@ServiceImpl
public class MacroExpansionManagerImpl
    implements MacroExpansionManager,
    PersistentStateComponent<MacroExpansionManagerImpl.PersistentState>,
    Disposable {

    @Nonnull
    private final Project myProject;

    private volatile boolean isDisposed = false;

    @Inject

    public MacroExpansionManagerImpl(@Nonnull Project project) {
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
    public void loadState(@Nonnull PersistentState state) {
        // TODO: restore the expansion directory from the persisted state
    }

    public void noStateLoaded() {
        loadState(new PersistentState(null));
    }

    // --- MacroExpansionManager interface ---

    @Nullable
    @Override
    public VirtualFile getIndexableDirectory() {
        return null;
    }

    @Nonnull
    @Override
    public CachedValueProvider.Result<RsResult<MacroExpansion, GetMacroExpansionError>> getExpansionFor(@Nonnull RsPossibleMacroCall call) {
        return CachedValueProvider.Result.create(
            new RsResult.Err<>(GetMacroExpansionError.MacroExpansionEngineIsNotReady),
            consulo.component.util.ModificationTracker.EVER_CHANGED
        );
    }

    @Nullable
    @Override
    public RsPossibleMacroCall getExpandedFrom(@Nonnull RsExpandedElement element) {
        return null;
    }

    @Nullable
    @Override
    public RsMacroCall getIncludedFrom(@Nonnull RsFile file) {
        return null;
    }

    @Nullable
    @Override
    public PsiElement getContextOfMacroCallExpandedFrom(@Nonnull RsFile stubParent) {
        return null;
    }

    @Override
    public boolean isExpansionFileOfCurrentProject(@Nonnull VirtualFile file) {
        return false;
    }

    @Nullable
    @Override
    public Integer getCrateForExpansionFile(@Nonnull VirtualFile file) {
        return null;
    }

    @Override
    public void reexpand() {
    }

    @Nonnull
    @Override
    public MacroExpansionMode getMacroExpansionMode() {
        return MacroExpansionMode.OLD;
    }

    
    @Nonnull
    @Override
    public Disposable setUnitTestExpansionModeAndDirectory(
        @Nonnull MacroExpansionScope mode,
        @Nonnull String cacheDirectory,
        boolean clearCacheBeforeDispose
    ) {
        return () -> {};
    }

    
    @Override
    public void updateInUnitTestMode() {
    }

    
    @Override
    public void setMacroExpansionEnabled(boolean enabled) {
    }

    @Override
    public void dispose() {
        isDisposed = true;
    }
}
