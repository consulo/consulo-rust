/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.rust.RsBundle;
import org.rust.ide.intentions.util.macros.InvokeInside;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.BinaryOperator;
import org.rust.lang.core.psi.ext.LogicOp;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;
import org.rust.lang.utils.RsBooleanExpUtils;
import org.rust.lang.core.psi.ext.RsBinaryExprUtil;
import org.rust.lang.core.psi.ext.RsBinaryOpUtil;

public class DemorgansLawIntention extends RsElementBaseIntentionAction<DemorgansLawIntention.Context> {

    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.demorgan.s.law");
    }

    private void setTextForElement(RsBinaryExpr element) {
        BinaryOperator opType = RsBinaryExprUtil.getOperatorType(element);
        if (opType == LogicOp.AND) {
            setText(RsBundle.message("intention.name.demorgan.s.law.replace.with2"));
        } else if (opType == LogicOp.OR) {
            setText(RsBundle.message("intention.name.demorgan.s.law.replace.with"));
        } else {
            setText("");
        }
    }

    @Override
    public InvokeInside getAttributeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    public static class Context {
        public final RsBinaryExpr binaryExpr;
        public final BinaryOperator binaryOpType;

        public Context(RsBinaryExpr binaryExpr, BinaryOperator binaryOpType) {
            this.binaryExpr = binaryExpr;
            this.binaryOpType = binaryOpType;
        }
    }

    @Override
    public Context findApplicableContext(Project project, Editor editor, PsiElement element) {
        RsBinaryExpr binExpr = RsPsiJavaUtil.ancestorStrict(element, RsBinaryExpr.class);
        if (binExpr == null) return null;
        BinaryOperator opType = RsBinaryExprUtil.getOperatorType(binExpr);
        if (opType instanceof LogicOp) {
            setTextForElement(binExpr);
            return new Context(binExpr, opType);
        }
        return null;
    }

    @Override
    public void invoke(Project project, Editor editor, Context ctx) {
        RsBinaryExpr topBinaryExpr = getTopmostBinaryExprWithSameOpType(ctx.binaryExpr);
        applyDemorgan(project, topBinaryExpr, ctx.binaryOpType);
    }

    private RsBinaryExpr getTopmostBinaryExprWithSameOpType(RsBinaryExpr expr) {
        PsiElement parent = expr.getParent();
        if (parent instanceof RsBinaryExpr) {
            RsBinaryExpr parentBin = (RsBinaryExpr) parent;
            if (RsBinaryExprUtil.getOperatorType(parentBin) == RsBinaryExprUtil.getOperatorType(expr)) {
                return getTopmostBinaryExprWithSameOpType(parentBin);
            }
        }
        return expr;
    }

    private void applyDemorgan(Project project, RsBinaryExpr topBinExpr, BinaryOperator opType) {
        String converted = convertConjunctionExpression(topBinExpr, opType);
        if (converted == null) return;

        PsiElement parent = topBinExpr.getParent() != null ? topBinExpr.getParent().getParent() : null;
        String expString;
        RsExpr expressionToReplace;

        if (parent != null && RsBooleanExpUtils.isNegation(parent)) {
            PsiElement grandParent = parent.getParent();
            LogicOp convertedOpType = opType == LogicOp.OR ? LogicOp.AND : LogicOp.OR;
            boolean canOmitParens = canOmitParensFor(grandParent, convertedOpType);
            expString = canOmitParens ? converted : "(" + converted + ")";
            expressionToReplace = (RsExpr) parent;
        } else {
            expString = "!(" + converted + ")";
            expressionToReplace = topBinExpr;
        }
        RsExpr newExpr = new RsPsiFactory(project).createExpression(expString);
        expressionToReplace.replace(newExpr);
    }

    private boolean canOmitParensFor(PsiElement element, LogicOp opType) {
        if (!(element instanceof RsBinaryExpr)) return true;
        RsBinaryExpr binExpr = (RsBinaryExpr) element;
        BinaryOperator binOpType = RsBinaryExprUtil.getOperatorType(binExpr);
        if (binOpType == LogicOp.AND) return opType == LogicOp.AND;
        if (binOpType == LogicOp.OR) return true;
        return false;
    }

    private boolean isConjunctionExpression(RsExpr expression, BinaryOperator opType) {
        return expression instanceof RsBinaryExpr && RsBinaryExprUtil.getOperatorType((RsBinaryExpr) expression) == opType;
    }

    private String convertLeafExpression(RsExpr condition) {
        if (RsBooleanExpUtils.isNegation(condition)) {
            RsExpr inner = ((RsUnaryExpr) condition).getExpr();
            return inner != null ? inner.getText() : "";
        }

        if (condition instanceof RsParenExpr) {
            RsExpr c = ((RsParenExpr) condition).getExpr();
            int level = 1;
            while (c instanceof RsParenExpr) {
                level++;
                c = ((RsParenExpr) c).getExpr();
            }
            if (c instanceof RsBinaryExpr && !(RsBinaryExprUtil.getOperatorType((RsBinaryExpr) c) instanceof LogicOp)) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < level; i++) sb.append("(");
                sb.append(RsBooleanExpUtils.negateToString((RsBinaryExpr) c));
                for (int i = 0; i < level; i++) sb.append(")");
                return sb.toString();
            } else {
                return "!" + condition.getText();
            }
        }

        if (condition instanceof RsBinaryExpr) {
            return RsBooleanExpUtils.negateToString((RsBinaryExpr) condition);
        }

        return "!" + condition.getText();
    }

    private String convertConjunctionExpression(RsBinaryExpr exp, BinaryOperator opType) {
        RsExpr lhs = exp.getLeft();
        String lhsText;
        if (isConjunctionExpression(lhs, opType)) {
            lhsText = convertConjunctionExpression((RsBinaryExpr) lhs, opType);
        } else {
            lhsText = convertLeafExpression(lhs);
        }

        RsExpr rhs = exp.getRight();
        if (rhs == null) return null;

        String rhsText;
        if (isConjunctionExpression(rhs, opType)) {
            rhsText = convertConjunctionExpression((RsBinaryExpr) rhs, opType);
        } else {
            rhsText = convertLeafExpression(rhs);
        }

        String flippedConjunction;
        if (RsBinaryExprUtil.getOperatorType(exp) == opType) {
            flippedConjunction = opType == LogicOp.AND ? "||" : "&&";
        } else {
            flippedConjunction = RsBinaryExprUtil.getOperator(exp).getText();
        }

        return lhsText + " " + flippedConjunction + " " + rhsText;
    }
}
