/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.dataflow.move;

public enum InitKind {
    /** Deep init, even on panic */
    Deep,

    /** Only does a shallow init */
    Shallow,

    /** This doesn't initialize the variable on panic (and a panic is possible) */
    NonPanicPathOnly
}
