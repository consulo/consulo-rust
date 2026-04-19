/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public abstract class ArithmeticAssignmentOp extends AssignmentOp implements OverloadableBinaryOperator {
    private final String traitName;
    private final String itemName;
    private final String sign;

    private ArithmeticAssignmentOp(String traitName, String itemName, String sign) {
        this.traitName = traitName;
        this.itemName = itemName;
        this.sign = sign;
    }

    @NotNull @Override public String getTraitName() { return traitName; }
    @NotNull @Override public String getItemName() { return itemName; }
    @NotNull @Override public String getFnName() { return itemName; }
    @NotNull @Override public String getSign() { return sign; }

    public static final ArithmeticAssignmentOp ANDEQ = new ArithmeticAssignmentOp("BitAndAssign", "bitand_assign", "&=") {};
    public static final ArithmeticAssignmentOp OREQ = new ArithmeticAssignmentOp("BitOrAssign", "bitor_assign", "|=") {};
    public static final ArithmeticAssignmentOp PLUSEQ = new ArithmeticAssignmentOp("AddAssign", "add_assign", "+=") {};
    public static final ArithmeticAssignmentOp MINUSEQ = new ArithmeticAssignmentOp("SubAssign", "sub_assign", "-=") {};
    public static final ArithmeticAssignmentOp MULEQ = new ArithmeticAssignmentOp("MulAssign", "mul_assign", "*=") {};
    public static final ArithmeticAssignmentOp DIVEQ = new ArithmeticAssignmentOp("DivAssign", "div_assign", "/=") {};
    public static final ArithmeticAssignmentOp REMEQ = new ArithmeticAssignmentOp("RemAssign", "rem_assign", "%=") {};
    public static final ArithmeticAssignmentOp XOREQ = new ArithmeticAssignmentOp("BitXorAssign", "bitxor_assign", "^=") {};
    public static final ArithmeticAssignmentOp GTGTEQ = new ArithmeticAssignmentOp("ShrAssign", "shr_assign", ">>=") {};
    public static final ArithmeticAssignmentOp LTLTEQ = new ArithmeticAssignmentOp("ShlAssign", "shl_assign", "<<=") {};

    public static List<ArithmeticAssignmentOp> values() {
        return Arrays.asList(ANDEQ, OREQ, PLUSEQ, MINUSEQ, MULEQ, DIVEQ, REMEQ, XOREQ, GTGTEQ, LTLTEQ);
    }
}
