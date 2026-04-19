/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.settings;

import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.rust.cargo.toolchain.RustChannel;

import java.util.Collections;
import java.util.Map;

@State(name = "RustfmtProjectSettings", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public class RustfmtProjectSettingsService
    extends RsProjectSettingsServiceBase<RustfmtProjectSettingsService.RustfmtProjectSettings> {

    public RustfmtProjectSettingsService(@NotNull Project project) {
        super(project, new RustfmtProjectSettings());
    }

    @NotNull
    public String getAdditionalArguments() { return getState().additionalArguments; }

    @NotNull
    public RustChannel getChannel() { return getState().channel; }

    @NotNull
    public Map<String, String> getEnvs() { return getState().envs; }

    public boolean getUseRustfmt() { return getState().useRustfmt; }

    public boolean getRunRustfmtOnSave() { return getState().runRustfmtOnSave; }

    public static class RustfmtProjectSettings extends RsProjectSettingsBase<RustfmtProjectSettings> {
        public String additionalArguments = "";
        public RustChannel channel = RustChannel.DEFAULT;
        public Map<String, String> envs = Collections.emptyMap();
        public boolean useRustfmt = false;
        public boolean runRustfmtOnSave = false;

        @NotNull
        @Override
        public RustfmtProjectSettings copy() {
            RustfmtProjectSettings state = new RustfmtProjectSettings();
            state.additionalArguments = this.additionalArguments;
            state.channel = this.channel;
            state.envs = this.envs;
            state.useRustfmt = this.useRustfmt;
            state.runRustfmtOnSave = this.runRustfmtOnSave;
            return state;
        }
    }

    @NotNull
    @Override
    protected SettingsChangedEvent createSettingsChangedEvent(
        @NotNull RustfmtProjectSettings oldEvent,
        @NotNull RustfmtProjectSettings newEvent
    ) {
        return new SettingsChangedEvent(oldEvent, newEvent);
    }

    public static class SettingsChangedEvent extends SettingsChangedEventBase<RustfmtProjectSettings> {
        public SettingsChangedEvent(
            @NotNull RustfmtProjectSettings oldState,
            @NotNull RustfmtProjectSettings newState
        ) {
            super(oldState, newState);
        }
    }
}
