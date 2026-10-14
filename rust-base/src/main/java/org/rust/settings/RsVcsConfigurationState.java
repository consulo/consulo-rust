/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.settings;

import com.dslplatform.json.CompiledJson;

/**
 * Must have default constructor and mutable fields for deserialization.
 */
@CompiledJson
public class RsVcsConfigurationState {
    private boolean rustFmt = false;

    public boolean getRustFmt() {
        return rustFmt;
    }

    public void setRustFmt(boolean value) {
        this.rustFmt = value;
    }
}
