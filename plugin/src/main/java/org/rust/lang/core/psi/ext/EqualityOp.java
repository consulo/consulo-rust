/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.resolve.KnownItems;

import java.util.Arrays;
import java.util.List;

public abstract class EqualityOp extends BoolOp implements OverloadableBinaryOperator {
    private final String sign;

    private EqualityOp(String sign) { this.sign = sign; }

    @Nonnull @Override public String getTraitName() { return "PartialEq"; }
    @Nonnull @Override public String getItemName() { return "eq"; }
    @Nonnull @Override public String getFnName() { return "eq"; }
    @Nonnull @Override public String getSign() { return sign; }
    @Nullable @Override public RsTraitItem findTrait(@Nonnull KnownItems items) { return items.getPartialEq(); }

    public static final EqualityOp EQ = new EqualityOp("==") {};
    public static final EqualityOp EXCLEQ = new EqualityOp("!=") {};

    public static List<EqualityOp> values() { return Arrays.asList(EQ, EXCLEQ); }
}
