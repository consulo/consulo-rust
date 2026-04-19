/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing.paste;

public enum StoredPreference {
    YES,
    NO,
    ASK_EVERY_TIME;

    @Override
    public String toString() {
        switch (this) {
            case YES: return "Yes";
            case NO: return "No";
            case ASK_EVERY_TIME: return "Ask every time";
            default: return name();
        }
    }
}
