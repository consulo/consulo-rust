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

public abstract class EqualityOp extends BoolOp implements OverloadableBinaryOperator {
    private final String sign;

    private EqualityOp(String sign) { this.sign = sign; }

    @NotNull @Override public String getTraitName() { return "PartialEq"; }
    @NotNull @Override public String getItemName() { return "eq"; }
    @NotNull @Override public String getFnName() { return "eq"; }
    @NotNull @Override public String getSign() { return sign; }
    @Nullable @Override public RsTraitItem findTrait(@NotNull KnownItems items) { return items.getPartialEq(); }

    public static final EqualityOp EQ = new EqualityOp("==") {};
    public static final EqualityOp EXCLEQ = new EqualityOp("!=") {};

    public static List<EqualityOp> values() { return Arrays.asList(EQ, EXCLEQ); }
}
