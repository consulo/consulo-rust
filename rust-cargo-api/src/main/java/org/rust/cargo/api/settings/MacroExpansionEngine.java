/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.api.settings;

import com.dslplatform.json.CompiledJson;

@CompiledJson
public enum MacroExpansionEngine {
    DISABLED,
    /**
     * Cannot be selected by a user anymore; it exists so saved settings still load.
     */
    OLD,
    NEW
}
