/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.resolve.KnownItems;

import java.util.Arrays;
import java.util.List;
import org.rust.lang.core.psi.ext.*;

public abstract class ComparisonOp extends BoolOp implements OverloadableBinaryOperator {
    private final String sign;

    private ComparisonOp(String sign) { this.sign = sign; }

    @Nonnull @Override public String getTraitName() { return "PartialOrd"; }
    @Nonnull @Override public String getItemName() { return "partial_ord"; }
    @Nonnull @Override public String getFnName() { return "partial_cmp"; }
    @Nonnull @Override public String getSign() { return sign; }
    @Nullable @Override public RsTraitItem findTrait(@Nonnull KnownItems items) { return items.getPartialOrd(); }

    public static final ComparisonOp LT = new ComparisonOp("<") {};
    public static final ComparisonOp LTEQ = new ComparisonOp("<=") {};
    public static final ComparisonOp GT = new ComparisonOp(">") {};
    public static final ComparisonOp GTEQ = new ComparisonOp(">=") {};

    public static List<ComparisonOp> values() { return Arrays.asList(LT, LTEQ, GT, GTEQ); }
}
