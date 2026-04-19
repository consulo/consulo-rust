/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions;

import com.intellij.codeInsight.editorActions.JoinRawLinesHandlerDelegate;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.rust.lang.core.parser.RustParserDefinition;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.RsTokenType;
import org.rust.lang.core.psi.ext.PsiElementUtil;
import org.rust.lang.core.psi.ext.RsBlockExprUtil;
import org.rust.lang.core.psi.ext.RsBlockUtil;
import org.rust.lang.core.psi.ext.RsFunctionUtil;

public class RsJoinRawLinesHandler implements JoinRawLinesHandlerDelegate {

    /**
     * Executed when user presses Ctrl+Shift+J, before lines are joined.
     */
    @Override
    public int tryJoinRawLines(Document document, PsiFile file, int start, int end) {
        if (!(file instanceof RsFile)) return CANNOT_JOIN;
        if (start == 0) return CANNOT_JOIN;

        int tryJoinSingleExpressionBlock = tryJoinSingleExpressionBlock((RsFile) file, start);
        if (tryJoinSingleExpressionBlock != CANNOT_JOIN) return tryJoinSingleExpressionBlock;

        PsiElement leftElem = file.findElementAt(start);
        PsiElement leftPsi = leftElem != null ? org.rust.lang.doc.psi.ext.RsDocCommentUtil.getContainingDoc(leftElem) : null;
        if (leftPsi == null) return CANNOT_JOIN;
        PsiElement rightElem = file.findElementAt(end);
        PsiElement rightPsi = rightElem != null ? org.rust.lang.doc.psi.ext.RsDocCommentUtil.getContainingDoc(rightElem) : null;
        if (rightPsi == null) return CANNOT_JOIN;

        if (leftPsi != rightPsi) return CANNOT_JOIN;

        var elementType = PsiElementUtil.getElementType(leftPsi);
        if (elementType == RustParserDefinition.INNER_EOL_DOC_COMMENT || elementType == RustParserDefinition.OUTER_EOL_DOC_COMMENT) {
            return joinLineDocComment(document, start, end);
        }

        return CANNOT_JOIN;
    }

    private int joinLineDocComment(Document document, int start, int end) {
        String prefix = document.getCharsSequence().subSequence(end, end + 3).toString();
        if (!prefix.equals("///") && !prefix.equals("//!")) return CANNOT_JOIN;
        document.deleteString(start, end + prefix.length());
        return start;
    }

    private int tryJoinSingleExpressionBlock(RsFile file, int start) {
        PsiElement lbrace = file.findElementAt(start - 1);
        if (lbrace == null) return CANNOT_JOIN;
        if (PsiElementUtil.getElementType(lbrace) != RsElementTypes.LBRACE) return CANNOT_JOIN;

        if (!(lbrace.getParent() instanceof RsBlock block)) return CANNOT_JOIN;

        RsExprStmt tailStmt = RsBlockUtil.singleTailStmt(block);
        if (tailStmt == null) return CANNOT_JOIN;
        if (block.getNode().getChildren(RsTokenType.RS_COMMENTS).length > 0) {
            return CANNOT_JOIN;
        }

        RsPsiFactory psiFactory = new RsPsiFactory(file.getProject());
        PsiElement parent = block.getParent();
        if (parent instanceof RsBlockExpr blockExpr) {
            if (RsBlockExprUtil.isUnsafe(blockExpr) || RsBlockExprUtil.isAsync(blockExpr) ||
                RsBlockExprUtil.isTry(blockExpr) || RsBlockExprUtil.isConst(blockExpr)) {
                return CANNOT_JOIN;
            }
            PsiElement grandpa = blockExpr.getParent();
            PsiElement newExpr = blockExpr.replace(tailStmt.getExpr());
            if (grandpa instanceof RsMatchArm matchArm) {
                PsiElement lastChild = matchArm.getLastChild();
                if (lastChild == null || PsiElementUtil.getElementType(lastChild) != RsElementTypes.COMMA) {
                    matchArm.add(psiFactory.createComma());
                }
            }
            return newExpr.getTextOffset();
        }

        if (parent instanceof RsIfExpr || parent instanceof RsElseBranch) {
            RsBlockExpr newBlockExpr = psiFactory.createBlockExpr(tailStmt.getExpr().getText());
            RsBlock newBlock = newBlockExpr.getBlock();
            PsiElement replaced = block.replace(newBlock);
            return replaced.getTextOffset();
        }

        return CANNOT_JOIN;
    }

    @Override
    public int tryJoinLines(Document document, PsiFile file, int start, int end) {
        return CANNOT_JOIN;
    }
}
