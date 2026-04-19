/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.thir;

import org.jetbrains.annotations.NotNull;

public class ThirFieldPat {
    /** MirFieldIndex */
    public final int field;
    @NotNull
    public final ThirPat pattern;

    public ThirFieldPat(int field, @NotNull ThirPat pattern) {
        this.field = field;
        this.pattern = pattern;
    }

    public int getField() {
        return field;
    }

    @NotNull
    public ThirPat getPattern() {
        return pattern;
    }
}
