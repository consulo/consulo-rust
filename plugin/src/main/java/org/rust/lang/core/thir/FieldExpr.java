/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.thir;

import jakarta.annotation.Nonnull;

/** Represents the association of a field identifier and an expression. This is used in struct constructors. */
public class FieldExpr {
    /** MirFieldIndex */
    public final int name;
    @Nonnull
    public final ThirExpr expr;

    public FieldExpr(int name, @Nonnull ThirExpr expr) {
        this.name = name;
        this.expr = expr;
    }

    public int getName() {
        return name;
    }

    @Nonnull
    public ThirExpr getExpr() {
        return expr;
    }
}
