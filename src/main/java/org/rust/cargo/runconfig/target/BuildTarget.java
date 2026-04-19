/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.target;

public enum BuildTarget {
    LOCAL,
    REMOTE;

    public boolean isLocal() {
        return this == LOCAL;
    }

    public boolean isRemote() {
        return this == REMOTE;
    }
}
