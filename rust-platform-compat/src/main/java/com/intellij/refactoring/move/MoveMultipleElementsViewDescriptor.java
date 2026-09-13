package com.intellij.refactoring.move;

import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.psi.PsiElement;
import consulo.usage.BaseUsageViewDescriptor;
import consulo.usage.UsageViewBundle;
import consulo.usage.UsageViewUtil;

/** Usage-view description of a move refactoring: the elements being moved and the place they go to. */
public class MoveMultipleElementsViewDescriptor extends BaseUsageViewDescriptor {
    private final PsiElement[] elementsToMove;
    private final String targetName;

    public MoveMultipleElementsViewDescriptor(PsiElement[] elementsToMove, String targetName) {
        super(elementsToMove);
        this.elementsToMove = elementsToMove;
        this.targetName = targetName;
    }

    @Override
    public String getProcessedElementsHeader() {
        if (elementsToMove.length == 1) {
            return RefactoringLocalize.moveSingleElementElementsHeader(
                UsageViewUtil.getType(elementsToMove[0]), targetName).get();
        }
        return RefactoringLocalize.moveSpecifiedElements().get();
    }

    @Override
    public String getCodeReferencesText(int usagesCount, int filesCount) {
        return RefactoringLocalize.referencesInCodeTo0(targetName).get()
            + UsageViewBundle.getReferencesString(usagesCount, filesCount);
    }
}
