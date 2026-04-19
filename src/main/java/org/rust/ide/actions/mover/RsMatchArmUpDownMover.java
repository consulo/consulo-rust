/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions.mover;

import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsMatchArm;
import org.rust.lang.core.psi.RsMatchBody;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.ext.PsiElementUtil;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;

import java.util.ArrayList;
import java.util.List;

public class RsMatchArmUpDownMover extends RsLineMover {

    @Override
    protected PsiElement findMovableAncestor(PsiElement psi, RangeEndpoint endpoint) {
        if (RsStatementUpDownMover.isMovableElement(psi)) return null;
        return RsPsiJavaUtil.ancestorOrSelf(psi, RsMatchArm.class);
    }

    @Override
    protected boolean canApply(PsiElement firstMovableElement, PsiElement secondMovableElement) {
        RsMatchBody firstMatchBody = RsPsiJavaUtil.ancestorStrict(firstMovableElement, RsMatchBody.class);
        if (firstMatchBody == null) return false;
        RsMatchBody secondMatchBody = RsPsiJavaUtil.ancestorStrict(secondMovableElement, RsMatchBody.class);
        if (secondMatchBody == null) return false;
        return firstMatchBody == secondMatchBody;
    }

    @Override
    protected PsiElement findTargetElement(PsiElement sibling, boolean down) {
        if (isMovingOutOfBraceBlock(sibling, down)) {
            UpDownMoverTestMarks.MoveOutOfMatch.hit();
            return null;
        }
        return sibling;
    }

    @Override
    public void beforeMove(Editor editor, MoveInfo info, boolean down) {
        com.intellij.openapi.project.Project project = editor.getProject();
        if (project == null) return;
        RsPsiFactory psiFactory = new RsPsiFactory(project, true, false);
        List<PsiElement> elements = new ArrayList<>();
        if (info.toMove.firstElement != null) elements.add(info.toMove.firstElement);
        if (info.toMove.lastElement != null) elements.add(info.toMove.lastElement);
        if (info.toMove2.firstElement != null) elements.add(info.toMove2.firstElement);
        for (PsiElement element : elements) {
            if (!(element instanceof RsMatchArm matchArm)) continue;
            PsiElement parent = matchArm.getParent();
            if (!(parent instanceof RsMatchBody matchBody)) continue;
            PsiElement lastChild = matchBody.getLastChild();
            PsiElement prevSibling = lastChild != null ? PsiElementUtil.getPrevNonCommentSibling(lastChild) : null;
            if (prevSibling == matchArm &&
                PsiElementUtil.getElementType(matchArm.getLastChild()) != RsElementTypes.COMMA &&
                (matchArm.getExpr() == null || PsiElementUtil.getElementType(matchArm.getExpr().getLastChild()) != RsElementTypes.BLOCK)) {
                matchBody.addAfter(psiFactory.createComma(), matchArm);
            }
        }
        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument());
    }
}
