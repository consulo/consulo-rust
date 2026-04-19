/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.consts;

import org.jetbrains.annotations.Nullable;
import org.rust.lang.utils.evaluation.ConstExpr;

import java.util.Objects;

public class CtValue extends Const {
    private final ConstExpr.Value<?> myExpr;

    public CtValue(ConstExpr.Value<?> expr) {
        super();
        myExpr = expr;
    }

    public ConstExpr.Value<?> getExpr() {
        return myExpr;
    }

    @Nullable
    public static Boolean asBool(Const c) {
        if (!(c instanceof CtValue)) return null;
        ConstExpr.Value<?> expr = ((CtValue) c).myExpr;
        if (expr instanceof ConstExpr.Value.Bool) {
            return ((ConstExpr.Value.Bool) expr).getValue();
        }
        return null;
    }

    @Nullable
    public static Long asLong(Const c) {
        if (!(c instanceof CtValue)) return null;
        ConstExpr.Value<?> expr = ((CtValue) c).myExpr;
        if (expr instanceof ConstExpr.Value.Integer) {
            return ((ConstExpr.Value.Integer) expr).getValue();
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CtValue ctValue = (CtValue) o;
        return Objects.equals(myExpr, ctValue.myExpr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myExpr);
    }

    @Override
    public String toString() {
        return myExpr.toString();
    }
}
