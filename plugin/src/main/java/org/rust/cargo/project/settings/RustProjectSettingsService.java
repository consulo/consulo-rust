/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.settings;

import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.project.Project;
import consulo.language.psi.PsiManager;
import consulo.util.lang.ThreeState;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import consulo.ide.setting.ShowSettingsUtil;
import org.rust.cargo.project.configurable.RsProjectConfigurable;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.toolchain.RsToolchainProvider;

import java.nio.file.Paths;

@State(name = "RustProjectSettings", storages = {
    @Storage(StoragePathMacros.WORKSPACE_FILE),
    @Storage(value = "misc.xml", deprecated = true)
})
public class RustProjectSettingsService
    extends RsProjectSettingsServiceBase<RustProjectSettingsService.RustProjectSettings> {

    public RustProjectSettingsService(@Nonnull Project project) {
        super(project, new RustProjectSettings());
    }

    @Nullable
    public RsToolchainBase getToolchain() { return getState().getToolchain(); }

    @Nullable
    public String getExplicitPathToStdlib() { return getState().explicitPathToStdlib; }

    @Nonnull
    public ThreeState getAutoShowErrorsInEditor() { return ThreeState.fromBoolean(getState().autoShowErrorsInEditor); }

    public boolean getAutoUpdateEnabled() { return getState().autoUpdateEnabled; }

    public boolean getCompileAllTargets() { return getState().compileAllTargets; }

    public boolean getUseOffline() { return getState().useOffline; }

    @Nonnull
    public MacroExpansionEngine getMacroExpansionEngine() { return getState().macroExpansionEngine; }

    public boolean getDoctestInjectionEnabled() { return getState().doctestInjectionEnabled; }

    public static class RustProjectSettings extends RsProjectSettingsBase<RustProjectSettings> {
        public String toolchainHomeDirectory;
        public boolean autoShowErrorsInEditor = true;
        public boolean autoUpdateEnabled = true;
        public String explicitPathToStdlib;
        public boolean compileAllTargets = true;
        public boolean useOffline = false;
        public MacroExpansionEngine macroExpansionEngine = MacroExpansionEngine.NEW;
        public boolean doctestInjectionEnabled = true;

        @Nullable
        public RsToolchainBase getToolchain() {
            return toolchainHomeDirectory != null
                ? RsToolchainProvider.getToolchainStatic(Paths.get(toolchainHomeDirectory))
                : null;
        }

        public void setToolchain(@Nullable RsToolchainBase value) {
            toolchainHomeDirectory = value != null
                ? value.getLocation().toString().replace('\\', '/')
                : null;
        }

        @Nonnull
        @Override
        public RustProjectSettings copy() {
            RustProjectSettings state = new RustProjectSettings();
            state.toolchainHomeDirectory = this.toolchainHomeDirectory;
            state.autoShowErrorsInEditor = this.autoShowErrorsInEditor;
            state.autoUpdateEnabled = this.autoUpdateEnabled;
            state.explicitPathToStdlib = this.explicitPathToStdlib;
            state.compileAllTargets = this.compileAllTargets;
            state.useOffline = this.useOffline;
            state.macroExpansionEngine = this.macroExpansionEngine;
            state.doctestInjectionEnabled = this.doctestInjectionEnabled;
            return state;
        }
    }

    @Override
    public void loadState(@Nonnull RustProjectSettings state) {
        if (state.macroExpansionEngine == MacroExpansionEngine.OLD) {
            state.macroExpansionEngine = MacroExpansionEngine.NEW;
        }
        super.loadState(state);
    }

    @Override
    protected void notifySettingsChanged(@Nonnull SettingsChangedEventBase<RustProjectSettings> event) {
        super.notifySettingsChanged(event);

        // Check if doctestInjectionEnabled changed - flush injection cache
        if (!java.util.Objects.equals(event.getOldState().doctestInjectionEnabled, event.getNewState().doctestInjectionEnabled)) {
            PsiManager.getInstance(project).dropPsiCaches();
        }
    }

    @Nonnull
    @Override
    protected SettingsChangedEvent createSettingsChangedEvent(
        @Nonnull RustProjectSettings oldEvent,
        @Nonnull RustProjectSettings newEvent
    ) {
        return new SettingsChangedEvent(oldEvent, newEvent);
    }

    public static class SettingsChangedEvent extends SettingsChangedEventBase<RustProjectSettings> {
        public SettingsChangedEvent(
            @Nonnull RustProjectSettings oldState,
            @Nonnull RustProjectSettings newState
        ) {
            super(oldState, newState);
        }
    }

    /**
     * Show a dialog for toolchain configuration.
     */
    public void configureToolchain() {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, RsProjectConfigurable.class);
    }

    public enum MacroExpansionEngine {
        DISABLED,
        OLD, // OLD can't be selected by a user anymore, it exists for backcompat with saved user settings
        NEW
    }
}
