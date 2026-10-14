/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.api.settings;

import com.dslplatform.json.CompiledJson;
import jakarta.annotation.Nonnull;

@CompiledJson
public class RustProjectSettings extends RsProjectSettingsServiceBase.RsProjectSettingsBase<RustProjectSettings> {
    @RsProjectSettingsServiceBase.AffectsCargoMetadata
    public String toolchainHomeDirectory;
    public boolean autoShowErrorsInEditor = true;
    public boolean autoUpdateEnabled = true;
    /**
     * Stdlib is normally located through rustup; this is the escape hatch for toolchains
     * that were not installed with rustup.
     */
    @RsProjectSettingsServiceBase.AffectsCargoMetadata
    public String explicitPathToStdlib;
    @RsProjectSettingsServiceBase.AffectsHighlighting
    public boolean compileAllTargets = true;
    public boolean useOffline = false;
    public MacroExpansionEngine macroExpansionEngine = MacroExpansionEngine.NEW;
    @RsProjectSettingsServiceBase.AffectsHighlighting
    public boolean doctestInjectionEnabled = true;

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
