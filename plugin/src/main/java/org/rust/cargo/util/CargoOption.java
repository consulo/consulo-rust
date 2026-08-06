/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.util;

import java.util.Collections;
import java.util.List;

public record CargoOption(String name, String description) {
    public String getLongName() {
        return "--" + name;
    }

    public String getDescription() {
        return description;
    }
}
