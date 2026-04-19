/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions.mover;

import com.intellij.codeInsight.editorActions.moveUpDown.LineMover;
import com.intellij.codeInsight.editorActions.moveUpDown.LineRange;
import com.intellij.codeInsight.editorActions.moveUpDown.StatementUpDownMover;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.PsiElementUtil;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;
import org.rust.openapiext.Testmark;

public abstract class RsLineMover extends LineMover {

    @Override
    public boolean checkAvailable(Editor editor, PsiFile file, MoveInfo info, boolean down) {
        if (!(file instanceof RsFile)) return false;
        if (!super.checkAvailable(editor, file, info, down)) return false;
        LineRange originalRange = info.toMove;
        if (originalRange == null) return false;
        var psiRange = StatementUpDownMover.getElementRange(editor, file, originalRange);
        if (psiRange == null) return false;
        if (psiRange.first == null || psiRange.second == null) return false;

        PsiElement firstItem = findMovableAncestor(psiRange.first, RangeEndpoint.START);
        if (firstItem == null) return false;
        PsiElement lastItem = findMovableAncestor(psiRange.second, RangeEndpoint.END);
        if (lastItem == null) return false;

        if (!canApply(firstItem, lastItem)) {
            info.toMove2 = null;
            return true;
        }

        PsiElement sibling = StatementUpDownMover.firstNonWhiteElement(
            down ? lastItem.getNextSibling() : firstItem.getPrevSibling(),
            down
        );
        if (sibling != null) sibling = fixupSibling(sibling, down);
        if (sibling == null) {
            info.toMove2 = null;
            return true;
        }

        LineRange sourceRange = new LineRange(firstItem, lastItem);
        info.toMove = sourceRange;
        info.toMove.firstElement = firstItem;
        info.toMove.lastElement = lastItem;

        PsiWhiteSpace whitespace = findTargetWhitespace(sibling, down);

        if (whitespace != null) {
            int nearLine = down ? sourceRange.endLine : sourceRange.startLine - 1;
            info.toMove2 = new LineRange(nearLine, nearLine + 1);
            info.toMove2.firstElement = whitespace;
        } else {
            PsiElement target = findTargetElement(sibling, down);
            if (target != null) {
                info.toMove2 = new LineRange(target);
                info.toMove2.firstElement = target;
            } else {
                info.toMove2 = null;
            }
        }

        return true;
    }

    protected abstract PsiElement findMovableAncestor(PsiElement psi, RangeEndpoint endpoint);

    protected abstract PsiElement findTargetElement(PsiElement sibling, boolean down);

    protected PsiElement fixupSibling(PsiElement sibling, boolean down) {
        return sibling;
    }

    protected boolean canApply(PsiElement firstMovableElement, PsiElement secondMovableElement) {
        return true;
    }

    protected PsiWhiteSpace findTargetWhitespace(PsiElement sibling, boolean down) {
        return null;
    }

    public enum RangeEndpoint {
        START, END
    }

    public static boolean isMovingOutOfBraceBlock(PsiElement sibling, boolean down) {
        return PsiElementUtil.getElementType(sibling) == (down ? RsElementTypes.RBRACE : RsElementTypes.LBRACE);
    }

    public static boolean isMovingOutOfParenBlock(PsiElement sibling, boolean down) {
        return PsiElementUtil.getElementType(sibling) == (down ? RsElementTypes.RPAREN : RsElementTypes.LPAREN);
    }

    public static boolean isMovingOutOfBracketBlock(PsiElement sibling, boolean down) {
        return PsiElementUtil.getElementType(sibling) == (down ? RsElementTypes.RBRACK : RsElementTypes.LBRACK);
    }

    public static boolean isMovingOutOfFunctionBody(PsiElement sibling, boolean down) {
        return isMovingOutOfBraceBlock(sibling, down) &&
            sibling.getParent() != null &&
            sibling.getParent().getParent() instanceof RsFunction;
    }

    public static boolean isMovingOutOfMatchArmBlock(PsiElement sibling, boolean down) {
        return isMovingOutOfBraceBlock(sibling, down) &&
            sibling.getParent() != null &&
            sibling.getParent().getParent() != null &&
            sibling.getParent().getParent().getParent() instanceof RsMatchArm;
    }

    public static boolean isMovingOutOfBlockExprInsideArgList(PsiElement sibling, boolean down) {
        if (!isMovingOutOfBraceBlock(sibling, down)) return false;
        PsiElement blockExpr = RsPsiJavaUtil.ancestorStrict(sibling, RsBlockExpr.class);
        if (blockExpr == null) return false;
        return RsPsiJavaUtil.ancestorStrict(blockExpr, RsValueArgumentList.class) != null;
    }
}
