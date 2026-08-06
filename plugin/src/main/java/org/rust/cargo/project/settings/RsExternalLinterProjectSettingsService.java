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
import org.rust.cargo.toolchain.ExternalLinter;
import org.rust.cargo.toolchain.RustChannel;

import java.util.Collections;
import java.util.Map;

@State(name = "RsExternalLinterProjectSettings", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public class RsExternalLinterProjectSettingsService
    extends RsProjectSettingsServiceBase<RsExternalLinterProjectSettingsService.RsExternalLinterProjectSettings> {

    public RsExternalLinterProjectSettingsService(@Nonnull Project project) {
        super(project, new RsExternalLinterProjectSettings());
    }

    @Nonnull
    public ExternalLinter getTool() { return getState().tool; }

    @Nonnull
    public String getAdditionalArguments() { return getState().additionalArguments; }

    @Nonnull
    public RustChannel getChannel() { return getState().channel; }

    @Nonnull
    public Map<String, String> getEnvs() { return getState().envs; }

    public boolean getRunOnTheFly() { return getState().runOnTheFly; }

    public static class RsExternalLinterProjectSettings extends RsProjectSettingsBase<RsExternalLinterProjectSettings> {
        public ExternalLinter tool = ExternalLinter.DEFAULT;
        public String additionalArguments = "";
        public RustChannel channel = RustChannel.DEFAULT;
        public Map<String, String> envs = Collections.emptyMap();
        public boolean runOnTheFly = false;

        @Nonnull
        @Override
        public RsExternalLinterProjectSettings copy() {
            RsExternalLinterProjectSettings state = new RsExternalLinterProjectSettings();
            state.tool = this.tool;
            state.additionalArguments = this.additionalArguments;
            state.channel = this.channel;
            state.envs = this.envs;
            state.runOnTheFly = this.runOnTheFly;
            return state;
        }
    }

    @Nonnull
    @Override
    protected SettingsChangedEvent createSettingsChangedEvent(
        @Nonnull RsExternalLinterProjectSettings oldEvent,
        @Nonnull RsExternalLinterProjectSettings newEvent
    ) {
        return new SettingsChangedEvent(oldEvent, newEvent);
    }

    public static class SettingsChangedEvent extends SettingsChangedEventBase<RsExternalLinterProjectSettings> {
        public SettingsChangedEvent(
            @Nonnull RsExternalLinterProjectSettings oldState,
            @Nonnull RsExternalLinterProjectSettings newState
        ) {
            super(oldState, newState);
        }
    }
}
