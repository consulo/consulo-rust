/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

public enum VisibilityType {
    CfgDisabled,
    Invisible,
    Normal;

    public boolean isWider(VisibilityType other) {
        return ordinal() > other.ordinal();
    }
}
