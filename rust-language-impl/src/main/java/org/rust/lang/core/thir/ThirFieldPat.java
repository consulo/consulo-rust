/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.thir;

import jakarta.annotation.Nonnull;

public class ThirFieldPat {
    /** MirFieldIndex */
    public final int field;
    @Nonnull
    public final ThirPat pattern;

    public ThirFieldPat(int field, @Nonnull ThirPat pattern) {
        this.field = field;
        this.pattern = pattern;
    }

    public int getField() {
        return field;
    }

    @Nonnull
    public ThirPat getPattern() {
        return pattern;
    }
}
