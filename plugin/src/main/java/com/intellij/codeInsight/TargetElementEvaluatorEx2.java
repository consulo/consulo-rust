package com.intellij.codeInsight;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
/** IntelliJ-compat stub. */
public abstract class TargetElementEvaluatorEx2 {
    public PsiElement adjustReferenceOrReferencedElement(PsiElement element, PsiElement resolved) { return resolved; }
    public PsiElement adjustReference(PsiReference reference) { return null; }
    public PsiElement getNamedElement(PsiElement element) { return null; }
}
