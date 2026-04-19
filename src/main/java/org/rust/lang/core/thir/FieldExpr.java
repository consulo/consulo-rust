/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.thir;

import org.jetbrains.annotations.NotNull;

/** Represents the association of a field identifier and an expression. This is used in struct constructors. */
public class FieldExpr {
    /** MirFieldIndex */
    public final int name;
    @NotNull
    public final ThirExpr expr;

    public FieldExpr(int name, @NotNull ThirExpr expr) {
        this.name = name;
        this.expr = expr;
    }

    public int getName() {
        return name;
    }

    @NotNull
    public ThirExpr getExpr() {
        return expr;
    }
}
