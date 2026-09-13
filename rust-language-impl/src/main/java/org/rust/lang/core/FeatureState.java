/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * All variants can be used by deserialization.
 */
public enum FeatureState {
    /**
     * Represents active features that are currently being implemented or
     * currently being considered for addition/removal.
     * Such features can be used only with nightly compiler with the corresponding feature attribute
     */
    ACTIVE,

    /**
     * Represents incomplete features that may not be safe to use and/or cause compiler crashes.
     * Such features can be used only with nightly compiler with the corresponding feature attribute
     */
    INCOMPLETE,

    /**
     * Those language feature has since been Accepted (it was once Active)
     * so such language features can be used with stable/beta compiler since some version
     * without any additional attributes
     */
    ACCEPTED,

    /**
     * Represents unstable features which have since been removed (it was once Active)
     */
    REMOVED,

    /**
     * Represents stable features which have since been removed (it was once Accepted)
     */
    STABILIZED;

    @JsonValue
    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
