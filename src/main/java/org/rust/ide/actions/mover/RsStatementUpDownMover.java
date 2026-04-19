/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions.mover;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.tree.TokenSet;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.PsiElementUtil;
import org.rust.lang.core.psi.ext.RsFunctionUtil;

public class RsStatementUpDownMover extends RsLineMover {

    private static final TokenSet movableItems = TokenSet.create(
        RsElementTypes.STMT,
        RsElementTypes.EXPR_STMT,
        RsElementTypes.EMPTY_STMT,
        RsElementTypes.LET_DECL
    );

    private static boolean isBlockExpr(PsiElement element) {
        return element instanceof RsExpr && element.getParent() instanceof RsBlock;
    }

    private static boolean isComment(PsiElement element) {
        return RsTokenType.RS_COMMENTS.contains(PsiElementUtil.getElementType(element));
    }

    public static boolean isMovableElement(PsiElement element) {
        return movableItems.contains(PsiElementUtil.getElementType(element)) || isBlockExpr(element) || isComment(element);
    }

    @Override
    protected PsiElement findMovableAncestor(PsiElement psi, RangeEndpoint endpoint) {
        PsiElement current = psi;
        while (current != null) {
            if (isMovableElement(current)) return current;
            current = current.getParent();
        }
        return null;
    }

    @Override
    protected PsiElement findTargetElement(PsiElement sibling, boolean down) {
        if (isMovingOutOfFunctionBody(sibling, down) ||
            isMovingOutOfMatchArmBlock(sibling, down) ||
            isMovingOutOfBlockExprInsideArgList(sibling, down)
        ) {
            UpDownMoverTestMarks.MoveOutOfBody.hit();
            return null;
        }
        RsBlock block = getClosestBlock(sibling, down);
        if (block == null) return sibling;
        return down ? block.getLbrace() : block.getRbrace();
    }

    @Override
    protected PsiWhiteSpace findTargetWhitespace(PsiElement sibling, boolean down) {
        PsiElement adjacent = down ? sibling.getPrevSibling() : sibling.getNextSibling();
        if (!(adjacent instanceof PsiWhiteSpace whitespace)) return null;
        return PsiElementUtil.isMultiLine(whitespace) ? whitespace : null;
    }

    private RsBlock getClosestBlock(PsiElement element, boolean down) {
        if (element instanceof RsWhileExpr whileExpr) return whileExpr.getBlock();
        if (element instanceof RsLoopExpr loopExpr) return loopExpr.getBlock();
        if (element instanceof RsForExpr forExpr) return forExpr.getBlock();
        if (element instanceof RsBlockExpr blockExpr) return blockExpr.getBlock();
        if (element instanceof RsIfExpr ifExpr) {
            if (down) return ifExpr.getBlock();
            RsElseBranch elseBranch = ifExpr.getElseBranch();
            if (elseBranch != null && elseBranch.getBlock() != null) return elseBranch.getBlock();
            return ifExpr.getBlock();
        }
        if (element instanceof RsLambdaExpr lambdaExpr) {
            RsExpr expr = lambdaExpr.getExpr();
            if (expr != null) return getClosestBlock(expr, down);
        }
        if (element instanceof RsExprStmt exprStmt) {
            return getClosestBlock(exprStmt.getExpr(), down);
        }
        return null;
    }
}
