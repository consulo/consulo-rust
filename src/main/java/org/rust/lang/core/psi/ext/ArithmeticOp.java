/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public abstract class ArithmeticOp extends BinaryOperator implements OverloadableBinaryOperator {
    private final String traitName;
    private final String itemName;
    private final String sign;

    private ArithmeticOp(String traitName, String itemName, String sign) {
        this.traitName = traitName;
        this.itemName = itemName;
        this.sign = sign;
    }

    @NotNull @Override public String getTraitName() { return traitName; }
    @NotNull @Override public String getItemName() { return itemName; }
    @NotNull @Override public String getFnName() { return itemName; }
    @NotNull @Override public String getSign() { return sign; }

    public static final ArithmeticOp ADD = new ArithmeticOp("Add", "add", "+") {};
    public static final ArithmeticOp SUB = new ArithmeticOp("Sub", "sub", "-") {};
    public static final ArithmeticOp MUL = new ArithmeticOp("Mul", "mul", "*") {};
    public static final ArithmeticOp DIV = new ArithmeticOp("Div", "div", "/") {};
    public static final ArithmeticOp REM = new ArithmeticOp("Rem", "rem", "%") {};
    public static final ArithmeticOp BIT_AND = new ArithmeticOp("BitAnd", "bitand", "&") {};
    public static final ArithmeticOp BIT_OR = new ArithmeticOp("BitOr", "bitor", "|") {};
    public static final ArithmeticOp BIT_XOR = new ArithmeticOp("BitXor", "bitxor", "^") {};
    public static final ArithmeticOp SHL = new ArithmeticOp("Shl", "shl", "<<") {};
    public static final ArithmeticOp SHR = new ArithmeticOp("Shr", "shr", ">>") {};

    public static List<ArithmeticOp> values() {
        return Arrays.asList(ADD, SUB, MUL, DIV, REM, BIT_AND, BIT_OR, BIT_XOR, SHL, SHR);
    }
}
