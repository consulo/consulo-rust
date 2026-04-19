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
import org.rust.cargo.toolchain.ExternalLinter;
import org.rust.cargo.toolchain.RustChannel;

import java.util.Collections;
import java.util.Map;

@State(name = "RsExternalLinterProjectSettings", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public class RsExternalLinterProjectSettingsService
    extends RsProjectSettingsServiceBase<RsExternalLinterProjectSettingsService.RsExternalLinterProjectSettings> {

    public RsExternalLinterProjectSettingsService(@NotNull Project project) {
        super(project, new RsExternalLinterProjectSettings());
    }

    @NotNull
    public ExternalLinter getTool() { return getState().tool; }

    @NotNull
    public String getAdditionalArguments() { return getState().additionalArguments; }

    @NotNull
    public RustChannel getChannel() { return getState().channel; }

    @NotNull
    public Map<String, String> getEnvs() { return getState().envs; }

    public boolean getRunOnTheFly() { return getState().runOnTheFly; }

    public static class RsExternalLinterProjectSettings extends RsProjectSettingsBase<RsExternalLinterProjectSettings> {
        public ExternalLinter tool = ExternalLinter.DEFAULT;
        public String additionalArguments = "";
        public RustChannel channel = RustChannel.DEFAULT;
        public Map<String, String> envs = Collections.emptyMap();
        public boolean runOnTheFly = false;

        @NotNull
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

    @NotNull
    @Override
    protected SettingsChangedEvent createSettingsChangedEvent(
        @NotNull RsExternalLinterProjectSettings oldEvent,
        @NotNull RsExternalLinterProjectSettings newEvent
    ) {
        return new SettingsChangedEvent(oldEvent, newEvent);
    }

    public static class SettingsChangedEvent extends SettingsChangedEventBase<RsExternalLinterProjectSettings> {
        public SettingsChangedEvent(
            @NotNull RsExternalLinterProjectSettings oldState,
            @NotNull RsExternalLinterProjectSettings newState
        ) {
            super(oldState, newState);
        }
    }
}
