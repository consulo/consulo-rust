/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.api.settings;

import com.dslplatform.json.CompiledJson;
import jakarta.annotation.Nonnull;
import org.rust.cargo.api.toolchain.ExternalLinter;
import org.rust.cargo.api.toolchain.RustChannel;

import java.util.Collections;
import java.util.Map;

@CompiledJson
public class RsExternalLinterProjectSettings
    extends RsProjectSettingsServiceBase.RsProjectSettingsBase<RsExternalLinterProjectSettings> {
    @RsProjectSettingsServiceBase.AffectsHighlighting
    public ExternalLinter tool = ExternalLinter.DEFAULT;
    @RsProjectSettingsServiceBase.AffectsHighlighting
    public String additionalArguments = "";
    @RsProjectSettingsServiceBase.AffectsHighlighting
    public RustChannel channel = RustChannel.DEFAULT;
    @RsProjectSettingsServiceBase.AffectsHighlighting
    public Map<String, String> envs = Collections.emptyMap();
    @RsProjectSettingsServiceBase.AffectsHighlighting
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
