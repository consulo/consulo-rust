/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty;

public enum Unsafety {
    Unsafe,
    Normal;

    public static Unsafety fromBoolean(boolean unsafe) {
        return unsafe ? Unsafe : Normal;
    }
}
