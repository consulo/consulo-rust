/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.api.settings;

import org.rust.cargo.api.settings.RsProjectSettingsServiceBase;

import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.project.Project;
import consulo.language.psi.PsiManager;
import consulo.util.lang.ThreeState;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.annotation.component.ComponentScope;
import jakarta.inject.Inject;
import consulo.util.xml.serializer.annotation.Transient;

@State(name = "RustProjectSettings", storages = {
    @Storage(StoragePathMacros.WORKSPACE_FILE),
    @Storage(value = "misc.xml", deprecated = true)
})
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class RustProjectSettingsService
    extends RsProjectSettingsServiceBase<RustProjectSettings> {

    @Inject

    public RustProjectSettingsService(@Nonnull Project project) {
        super(project, new RustProjectSettings());
    }

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

}
