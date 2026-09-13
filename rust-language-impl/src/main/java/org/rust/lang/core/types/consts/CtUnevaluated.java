/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.consts;

import org.rust.lang.core.types.KindUtil;
import org.rust.lang.core.types.infer.TypeFolder;
import org.rust.lang.core.types.infer.TypeVisitor;
import org.rust.lang.utils.evaluation.ConstExpr;

import java.util.Objects;

public class CtUnevaluated extends Const {
    private final ConstExpr<?> myExpr;

    public CtUnevaluated(ConstExpr<?> expr) {
        super(KindUtil.HAS_CT_UNEVALUATED_MASK | expr.getFlags());
        myExpr = expr;
    }

    public ConstExpr<?> getExpr() {
        return myExpr;
    }

    @Override
    public Const superFoldWith(TypeFolder folder) {
        return new CtUnevaluated(myExpr.foldWith(folder));
    }

    @Override
    public boolean superVisitWith(TypeVisitor visitor) {
        return myExpr.visitWith(visitor);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CtUnevaluated that = (CtUnevaluated) o;
        return Objects.equals(myExpr, that.myExpr);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myExpr);
    }

    @Override
    public String toString() {
        return "<unknown>";
    }
}
