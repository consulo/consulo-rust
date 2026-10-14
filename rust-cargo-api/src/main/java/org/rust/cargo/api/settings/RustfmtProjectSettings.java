/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.api.settings;

import com.dslplatform.json.CompiledJson;
import jakarta.annotation.Nonnull;
import org.rust.cargo.api.toolchain.RustChannel;

import java.util.Collections;
import java.util.Map;

@CompiledJson
public class RustfmtProjectSettings extends RsProjectSettingsServiceBase.RsProjectSettingsBase<RustfmtProjectSettings> {
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
