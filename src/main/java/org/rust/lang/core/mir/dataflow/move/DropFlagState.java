/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.dataflow.move;

public enum DropFlagState {
    /** The tracked value is initialized and needs to be dropped when leaving its scope */
    Present,

    /** The tracked value is uninitialized or was moved out of and does not need to be dropped when leaving its scope */
    Absent
}
