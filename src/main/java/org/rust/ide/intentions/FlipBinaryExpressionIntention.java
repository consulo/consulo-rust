/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.rust.RsBundle;
import org.rust.ide.intentions.util.macros.InvokeInside;
import org.rust.ide.utils.PsiModificationUtil;
import org.rust.lang.core.psi.RsBinaryExpr;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;

import java.util.List;
import org.rust.lang.core.psi.ext.RsBinaryExprUtil;

public class FlipBinaryExpressionIntention extends RsElementBaseIntentionAction<RsBinaryExpr> {

    @Override
    public String getText() {
        return RsBundle.message("intention.name.flip.binary.expression");
    }

    @Override
    public String getFamilyName() {
        return getText();
    }

    @Override
    public InvokeInside getAttributeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    private static final List<IElementType> COMMUNICATIVE_OPERATORS = List.of(
        RsElementTypes.PLUS, RsElementTypes.MUL, RsElementTypes.AND,
        RsElementTypes.OR, RsElementTypes.XOR, RsElementTypes.EQEQ, RsElementTypes.EXCLEQ
    );

    private static final List<IElementType> CHANGE_SEMANTICS_OPERATORS = List.of(
        RsElementTypes.MINUS, RsElementTypes.DIV, RsElementTypes.REM,
        RsElementTypes.ANDAND, RsElementTypes.OROR, RsElementTypes.GTGT, RsElementTypes.LTLT
    );

    private static final List<IElementType> COMPARISON_OPERATORS = List.of(
        RsElementTypes.GT, RsElementTypes.GTEQ, RsElementTypes.LT, RsElementTypes.LTEQ
    );

    @Override
    public RsBinaryExpr findApplicableContext(Project project, Editor editor, PsiElement element) {
        RsBinaryExpr binaryExpr = RsPsiJavaUtil.ancestorStrict(element, RsBinaryExpr.class);
        if (binaryExpr == null) return null;
        if (element.getParent() != binaryExpr.getBinaryOp()) return null;
        if (binaryExpr.getRight() == null) return null;
        if (!PsiModificationUtil.canReplace(binaryExpr)) return null;
        PsiElement op = RsBinaryExprUtil.getOperator(binaryExpr);
        String opText = op.getText();
        IElementType opType = RsPsiJavaUtil.elementType(op);

        if (COMMUNICATIVE_OPERATORS.contains(opType)) {
            setText(RsBundle.message("intention.name.flip", opText));
        } else if (CHANGE_SEMANTICS_OPERATORS.contains(opType)) {
            setText(RsBundle.message("intention.name.flip.changes.semantics", opText));
        } else if (COMPARISON_OPERATORS.contains(opType)) {
            setText(RsBundle.message("intention.name.flip.to", opText, flippedOp(opText)));
        } else {
            return null;
        }
        return binaryExpr;
    }

    @Override
    public void invoke(Project project, Editor editor, RsBinaryExpr ctx) {
        if (ctx.getRight() == null) return;
        String right = ctx.getRight().getText();
        String left = ctx.getLeft().getText();
        String op = flippedOp(RsBinaryExprUtil.getOperator(ctx).getText());
        ctx.replace(new RsPsiFactory(project).createExpression(right + " " + op + " " + left));
    }

    public static String flippedOp(String op) {
        switch (op) {
            case ">": return "<";
            case ">=": return "<=";
            case "<": return ">";
            case "<=": return ">=";
            default: return op;
        }
    }
}
