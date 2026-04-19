/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.utils;

import com.intellij.openapi.project.Project;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.consts.CtValue;
import org.rust.lang.core.types.ty.TyBool;
import org.rust.lang.utils.evaluation.EvaluateUtil;
import org.rust.lang.utils.NegateUtil;

import java.util.Set;
import org.rust.lang.core.psi.ext.RsUnaryExprUtil;
import org.rust.lang.core.psi.ext.RsBinaryExprUtil;
import org.rust.lang.utils.evaluation.ConstExprEvaluator;

public class BooleanExprSimplifier {
    private final Project myProject;
    private final RsPsiFactory myFactory;

    public BooleanExprSimplifier(Project project) {
        this.myProject = project;
        this.myFactory = new RsPsiFactory(project);
    }

    public Project getProject() {
        return myProject;
    }

    /**
     * Simplifies a boolean expression if can.
     *
     * @return {@code null} if expr cannot be simplified, {@code expr} otherwise
     */
    public RsExpr simplify(RsExpr expr) {
        if (expr instanceof RsLitExpr) return null;

        Boolean value = eval(expr);
        if (value != null) {
            return myFactory.createExpression(value.toString());
        }

        if (expr instanceof RsBinaryExpr) {
            RsBinaryExpr binExpr = (RsBinaryExpr) expr;
            RsExpr left = binExpr.getLeft();
            RsExpr right = binExpr.getRight();
            if (right == null) return null;
            BinaryOperator op = RsBinaryExprUtil.getOperatorType(binExpr);

            RsExpr lhs = simplify(left);
            if (lhs == null) lhs = left;
            RsExpr rhs = simplify(right);
            if (rhs == null) rhs = right;

            if (lhs instanceof RsLitExpr) {
                return simplifyBinaryOperation(op, (RsLitExpr) lhs, rhs);
            } else if (rhs instanceof RsLitExpr) {
                return simplifyBinaryOperation(op, (RsLitExpr) rhs, lhs);
            } else {
                return myFactory.createExpression(lhs.getText() + " " + binExpr.getBinaryOp().getText() + " " + rhs.getText());
            }
        } else if (expr instanceof RsUnaryExpr) {
            RsUnaryExpr unaryExpr = (RsUnaryExpr) expr;
            RsExpr inner = unaryExpr.getExpr();
            if (!(inner instanceof RsParenExpr)) return expr;
            RsParenExpr parenExpr = (RsParenExpr) inner;
            RsExpr interior = parenExpr.getExpr();
            if (RsUnaryExprUtil.getOperatorType(unaryExpr) == UnaryOperator.NOT && interior instanceof RsBinaryExpr) {
                return (RsExpr) NegateUtil.negate((RsBinaryExpr) interior);
            } else {
                return null;
            }
        } else if (expr instanceof RsParenExpr) {
            RsParenExpr parenExpr = (RsParenExpr) expr;
            RsExpr wrapped = parenExpr.getExpr();
            if (wrapped != null) {
                RsExpr interiorSimplified = simplify(wrapped);
                if (interiorSimplified != null) {
                    return myFactory.createExpression("(" + interiorSimplified.getText() + ")");
                }
            }
            return null;
        } else {
            return null;
        }
    }

    private RsExpr simplifyBinaryOperation(BinaryOperator op, RsLitExpr constExpr, RsExpr expr) {
        if (constExpr.getBoolLiteral() == null) return null;
        String literal = constExpr.getBoolLiteral().getText();
        if (op == LogicOp.AND) {
            return "false".equals(literal) ? myFactory.createExpression("false") : expr;
        } else if (op == LogicOp.OR) {
            return "true".equals(literal) ? myFactory.createExpression("true") : expr;
        } else if (op == EqualityOp.EQ) {
            return "false".equals(literal) ? myFactory.createExpression("!" + expr.getText()) : expr;
        } else if (op == EqualityOp.EXCLEQ) {
            return "true".equals(literal) ? myFactory.createExpression("!" + expr.getText()) : expr;
        } else {
            return null;
        }
    }

    public static boolean canBeSimplified(RsExpr expr) {
        if (expr instanceof RsLitExpr) return false;

        if (canBeEvaluated(expr)) return true;

        if (expr instanceof RsBinaryExpr) {
            RsBinaryExpr binExpr = (RsBinaryExpr) expr;
            RsExpr left = binExpr.getLeft();
            RsExpr right = binExpr.getRight();
            if (right == null) return false;

            BinaryOperator opType = RsBinaryExprUtil.getOperatorType(binExpr);
            Set<BinaryOperator> ops = Set.of(LogicOp.AND, LogicOp.OR, EqualityOp.EQ, EqualityOp.EXCLEQ);
            if (ops.contains(opType)) {
                if (canBeSimplified(left) || canBeSimplified(right)) return true;
                if (canBeEvaluated(left) || canBeEvaluated(right)) return true;
            }
        } else if (expr instanceof RsParenExpr) {
            RsParenExpr parenExpr = (RsParenExpr) expr;
            RsExpr wrapped = parenExpr.getExpr();
            return wrapped != null && canBeSimplified(wrapped);
        } else if (expr instanceof RsUnaryExpr) {
            RsUnaryExpr unaryExpr = (RsUnaryExpr) expr;
            if (RsUnaryExprUtil.getOperatorType(unaryExpr) != UnaryOperator.NOT) {
                return false;
            }
            RsExpr inner = unaryExpr.getExpr();
            if (!(inner instanceof RsParenExpr)) return false;
            RsExpr parenInner = ((RsParenExpr) inner).getExpr();
            if (!(parenInner instanceof RsBinaryExpr)) return false;
            BinaryOperator binOp = RsBinaryExprUtil.getOperatorType((RsBinaryExpr) parenInner);
            return binOp instanceof EqualityOp || binOp instanceof ComparisonOp;
        }

        return false;
    }

    private static boolean canBeEvaluated(RsExpr expr) {
        return eval(expr) != null;
    }

    private static Boolean eval(RsExpr expr) {
        return CtValue.asBool(ConstExprEvaluator.evaluate(expr, TyBool.INSTANCE, null));
    }
}
