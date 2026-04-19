/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions.ui;

public record CargoNewCrateSettings(boolean binary, String crateName) {
    public boolean getBinary() {
        return binary;
    }

    public String getCrateName() {
        return crateName;
    }
}
