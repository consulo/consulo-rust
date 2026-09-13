/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty;

public enum BorrowKind {
    /** {@code &expr} or {@code &mut expr} */
    REF,

    /** {@code &raw const expr} or {@code &raw mut expr} */
    RAW
}
