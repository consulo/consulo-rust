/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.workspace;

public enum FeatureState {
    Enabled,
    Disabled;

    public boolean isEnabled() {
        switch (this) {
            case Enabled:
                return true;
            case Disabled:
                return false;
            default:
                throw new IllegalStateException();
        }
    }

    public FeatureState not() {
        switch (this) {
            case Enabled:
                return Disabled;
            case Disabled:
                return Enabled;
            default:
                throw new IllegalStateException();
        }
    }
}
