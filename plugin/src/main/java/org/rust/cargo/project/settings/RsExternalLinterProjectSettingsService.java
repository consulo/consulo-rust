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
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.annotation.component.ComponentScope;
import jakarta.inject.Inject;

@State(name = "RsExternalLinterProjectSettings", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public class RsExternalLinterProjectSettingsService
    extends RsProjectSettingsServiceBase<RsExternalLinterProjectSettingsService.RsExternalLinterProjectSettings> {

    @Inject

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
        @AffectsHighlighting
        public ExternalLinter tool = ExternalLinter.DEFAULT;
        @AffectsHighlighting
        public String additionalArguments = "";
        @AffectsHighlighting
        public RustChannel channel = RustChannel.DEFAULT;
        @AffectsHighlighting
        public Map<String, String> envs = Collections.emptyMap();
        @AffectsHighlighting
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
