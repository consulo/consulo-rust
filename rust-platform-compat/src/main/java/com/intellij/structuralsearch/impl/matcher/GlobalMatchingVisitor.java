package com.intellij.structuralsearch.impl.matcher;
import com.intellij.structuralsearch.impl.matcher.handlers.MatchingHandler;
import com.intellij.structuralsearch.impl.matcher.handlers.SubstitutionHandler;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;

/** Structural-search visitor that matches a pattern against PSI elements. */
public class GlobalMatchingVisitor extends PsiElementVisitor {
    protected PsiElement myMatchedNode;
    protected PsiElement myElement;
    public void setResult(boolean result) {}
    public boolean getResult() { return false; }
    public PsiElement getElement() { return myElement; }
    public boolean match(PsiElement a, PsiElement b) { return false; }
    public boolean matchSequentially(PsiElement patternNode, PsiElement matchNode) { return false; }
    public boolean matchOptionally(PsiElement patternNode, PsiElement matchNode) { return false; }
    public boolean handleTypedElement(PsiElement pattern, PsiElement element) { return false; }
    public MatchingHandler getMatchingHandler(PsiElement node) { return null; }
    public MatchContext getMatchContext() { return new MatchContext(); }
    public boolean matchText(PsiElement a, PsiElement b) { return false; }
    public boolean matchSequentially(PsiElement[] a, PsiElement[] b) { return false; }
    public boolean matchInAnyOrder(PsiElement[] a, PsiElement[] b) { return false; }

    /** State of a structural-search match, giving access to the compiled pattern. */
    public static class MatchContext {
        public Pattern getPattern() { return new Pattern(); }
    }

    /** Compiled structural-search pattern holding the matching handler of each node. */
    public static class Pattern {
        public MatchingHandler getHandler(PsiElement element) { return null; }
    }
}
