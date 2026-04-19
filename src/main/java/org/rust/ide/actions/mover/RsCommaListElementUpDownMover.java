/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions.mover;

import com.intellij.codeInsight.editorActions.moveUpDown.StatementUpDownMover;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import org.rust.ide.formatter.impl.RsFmtImplUtil;
import org.rust.ide.formatter.processors.RsTrailingCommaFormatProcessor;
import org.rust.lang.core.psi.RsBlockExpr;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsMatchArm;
import org.rust.lang.core.psi.ext.PsiElementUtil;

import java.util.ArrayList;
import java.util.List;

public class RsCommaListElementUpDownMover extends RsLineMover {

    @Override
    protected PsiElement findMovableAncestor(PsiElement psi, RangeEndpoint endpoint) {
        if (psi instanceof RsMatchArm) return null;

        PsiElement current = psi;
        if (endpoint == RangeEndpoint.END && PsiElementUtil.getElementType(psi) == RsElementTypes.COMMA) {
            current = psi.getPrevSibling();
        }

        return findListElement(current);
    }

    private PsiElement findListElement(PsiElement psi) {
        PsiElement child = psi;
        PsiElement parent = psi.getParent();
        while (parent != null) {
            if (parent instanceof RsBlockExpr) return null;
            RsFmtImplUtil.CommaList list = RsFmtImplUtil.CommaList.forElement(PsiElementUtil.getElementType(parent));
            if (list != null && list.getIsElement().test(child)) return child;
            child = parent;
            parent = parent.getParent();
        }
        return null;
    }

    @Override
    protected PsiElement fixupSibling(PsiElement sibling, boolean down) {
        if (PsiElementUtil.getElementType(sibling) == RsElementTypes.COMMA) {
            return StatementUpDownMover.firstNonWhiteElement(
                down ? sibling.getNextSibling() : sibling.getPrevSibling(), down
            );
        }
        return sibling;
    }

    @Override
    protected PsiElement findTargetElement(PsiElement sibling, boolean down) {
        if (isMovingOutOfParenBlock(sibling, down) ||
            isMovingOutOfBraceBlock(sibling, down) ||
            isMovingOutOfBracketBlock(sibling, down)
        ) {
            UpDownMoverTestMarks.MoveOutOfBlock.hit();
            return null;
        }
        return sibling;
    }

    @Override
    public void beforeMove(Editor editor, MoveInfo info, boolean down) {
        com.intellij.openapi.project.Project project = editor.getProject();
        if (project == null) return;
        List<PsiElement> elements = new ArrayList<>();
        if (info.toMove.firstElement != null) elements.add(info.toMove.firstElement);
        if (info.toMove.lastElement != null) elements.add(info.toMove.lastElement);
        if (info.toMove2.firstElement != null) elements.add(info.toMove2.firstElement);
        for (PsiElement element : elements) {
            PsiElement list = element.getParent();
            RsFmtImplUtil.CommaList commaList = RsFmtImplUtil.CommaList.forElement(PsiElementUtil.getElementType(list));
            if (commaList == null) continue;
            if (RsTrailingCommaFormatProcessor.isOnSameLineAsLastElement(commaList, list, element)) {
                RsTrailingCommaFormatProcessor.addTrailingCommaForElement(commaList, list);
            }
        }
        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument());
    }
}
