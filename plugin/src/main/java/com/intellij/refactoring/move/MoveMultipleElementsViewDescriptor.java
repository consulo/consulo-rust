package com.intellij.refactoring.move;
import consulo.usage.UsageViewDescriptor;
import consulo.language.psi.PsiElement;
public class MoveMultipleElementsViewDescriptor implements UsageViewDescriptor {
    public MoveMultipleElementsViewDescriptor(PsiElement[] elementsToMove, String targetName) {}
    @Override public PsiElement[] getElements() { return new PsiElement[0]; }
    @Override public String getProcessedElementsHeader() { return ""; }
    @Override public String getCodeReferencesText(int usagesCount, int filesCount) { return ""; }
    @Override public String getCommentReferencesText(int usagesCount, int filesCount) { return ""; }
}
