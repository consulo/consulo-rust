/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsBinaryOp;
import org.rust.lang.core.psi.RsElementTypes;

/**
 * Utility methods for RsBinaryOp - provides getOperator and getOperatorType implementations.
 */
public final class RsBinaryOpImplUtil {

    private RsBinaryOpImplUtil() {
    }

    @NotNull
    public static PsiElement getOperator(@NotNull RsBinaryOp binaryOp) {
        // The operator is the first non-whitespace child element
        PsiElement child = binaryOp.getFirstChild();
        if (child != null) return child;
        return binaryOp;
    }

    @NotNull
    public static BinaryOperator getOperatorType(@NotNull RsBinaryOp binaryOp) {
        IElementType elementType = PsiElementExt.getElementType(getOperator(binaryOp));
        BinaryOperator result = operatorTypeFromElementType(elementType);
        if (result != null) return result;
        // Fallback: should never happen in valid code
        return AssignmentOp.EQ;
    }

    @Nullable
    private static BinaryOperator operatorTypeFromElementType(@NotNull IElementType elementType) {
        // Arithmetic operators
        if (elementType == RsElementTypes.PLUS) return ArithmeticOp.ADD;
        if (elementType == RsElementTypes.MINUS) return ArithmeticOp.SUB;
        if (elementType == RsElementTypes.MUL) return ArithmeticOp.MUL;
        if (elementType == RsElementTypes.DIV) return ArithmeticOp.DIV;
        if (elementType == RsElementTypes.REM) return ArithmeticOp.REM;
        if (elementType == RsElementTypes.AND) return ArithmeticOp.BIT_AND;
        if (elementType == RsElementTypes.OR) return ArithmeticOp.BIT_OR;
        if (elementType == RsElementTypes.XOR) return ArithmeticOp.BIT_XOR;
        if (elementType == RsElementTypes.LTLT) return ArithmeticOp.SHL;
        if (elementType == RsElementTypes.GTGT) return ArithmeticOp.SHR;

        // Assignment
        if (elementType == RsElementTypes.EQ) return AssignmentOp.EQ;

        // Arithmetic assignment operators
        if (elementType == RsElementTypes.ANDEQ) return ArithmeticAssignmentOp.ANDEQ;
        if (elementType == RsElementTypes.OREQ) return ArithmeticAssignmentOp.OREQ;
        if (elementType == RsElementTypes.PLUSEQ) return ArithmeticAssignmentOp.PLUSEQ;
        if (elementType == RsElementTypes.MINUSEQ) return ArithmeticAssignmentOp.MINUSEQ;
        if (elementType == RsElementTypes.MULEQ) return ArithmeticAssignmentOp.MULEQ;
        if (elementType == RsElementTypes.DIVEQ) return ArithmeticAssignmentOp.DIVEQ;
        if (elementType == RsElementTypes.REMEQ) return ArithmeticAssignmentOp.REMEQ;
        if (elementType == RsElementTypes.XOREQ) return ArithmeticAssignmentOp.XOREQ;
        if (elementType == RsElementTypes.GTGTEQ) return ArithmeticAssignmentOp.GTGTEQ;
        if (elementType == RsElementTypes.LTLTEQ) return ArithmeticAssignmentOp.LTLTEQ;

        // Equality operators
        if (elementType == RsElementTypes.EQEQ) return EqualityOp.EQ;
        if (elementType == RsElementTypes.EXCLEQ) return EqualityOp.EXCLEQ;

        // Comparison operators
        if (elementType == RsElementTypes.LT) return ComparisonOp.LT;
        if (elementType == RsElementTypes.LTEQ) return ComparisonOp.LTEQ;
        if (elementType == RsElementTypes.GT) return ComparisonOp.GT;
        if (elementType == RsElementTypes.GTEQ) return ComparisonOp.GTEQ;

        // Logical operators
        if (elementType == RsElementTypes.ANDAND) return LogicOp.AND;
        if (elementType == RsElementTypes.OROR) return LogicOp.OR;

        return null;
    }
}
