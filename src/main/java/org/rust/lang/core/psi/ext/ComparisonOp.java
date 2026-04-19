/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.resolve.KnownItems;

import java.util.Arrays;
import java.util.List;

public abstract class ComparisonOp extends BoolOp implements OverloadableBinaryOperator {
    private final String sign;

    private ComparisonOp(String sign) { this.sign = sign; }

    @NotNull @Override public String getTraitName() { return "PartialOrd"; }
    @NotNull @Override public String getItemName() { return "partial_ord"; }
    @NotNull @Override public String getFnName() { return "partial_cmp"; }
    @NotNull @Override public String getSign() { return sign; }
    @Nullable @Override public RsTraitItem findTrait(@NotNull KnownItems items) { return items.getPartialOrd(); }

    public static final ComparisonOp LT = new ComparisonOp("<") {};
    public static final ComparisonOp LTEQ = new ComparisonOp("<=") {};
    public static final ComparisonOp GT = new ComparisonOp(">") {};
    public static final ComparisonOp GTEQ = new ComparisonOp(">=") {};

    public static List<ComparisonOp> values() { return Arrays.asList(LT, LTEQ, GT, GTEQ); }
}
