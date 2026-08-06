/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.util;

public enum StdLibType {
    /**
     * An indispensable part of the stdlib
     */
    ROOT,

    /**
     * A crate that can be used as a dependency if a corresponding feature is turned on
     */
    FEATURE_GATED,

    /**
     * A dependency that is not visible outside of the stdlib
     */
    DEPENDENCY
}
