/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.settings;

import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.persist.StoragePathMacros;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import org.rust.cargo.toolchain.RustChannel;

import java.util.Collections;
import java.util.Map;

@State(name = "RustfmtProjectSettings", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public class RustfmtProjectSettingsService
    extends RsProjectSettingsServiceBase<RustfmtProjectSettingsService.RustfmtProjectSettings> {

    public RustfmtProjectSettingsService(@Nonnull Project project) {
        super(project, new RustfmtProjectSettings());
    }

    @Nonnull
    public String getAdditionalArguments() { return getState().additionalArguments; }

    @Nonnull
    public RustChannel getChannel() { return getState().channel; }

    @Nonnull
    public Map<String, String> getEnvs() { return getState().envs; }

    public boolean getUseRustfmt() { return getState().useRustfmt; }

    public boolean getRunRustfmtOnSave() { return getState().runRustfmtOnSave; }

    public static class RustfmtProjectSettings extends RsProjectSettingsBase<RustfmtProjectSettings> {
        public String additionalArguments = "";
        public RustChannel channel = RustChannel.DEFAULT;
        public Map<String, String> envs = Collections.emptyMap();
        public boolean useRustfmt = false;
        public boolean runRustfmtOnSave = false;

        @Nonnull
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

    @Nonnull
    @Override
    protected SettingsChangedEvent createSettingsChangedEvent(
        @Nonnull RustfmtProjectSettings oldEvent,
        @Nonnull RustfmtProjectSettings newEvent
    ) {
        return new SettingsChangedEvent(oldEvent, newEvent);
    }

    public static class SettingsChangedEvent extends SettingsChangedEventBase<RustfmtProjectSettings> {
        public SettingsChangedEvent(
            @Nonnull RustfmtProjectSettings oldState,
            @Nonnull RustfmtProjectSettings newState
        ) {
            super(oldState, newState);
        }
    }
}
