package com.intellij.structuralsearch.impl.matcher;
import com.intellij.structuralsearch.impl.matcher.handlers.MatchingHandler;
import com.intellij.structuralsearch.impl.matcher.handlers.SubstitutionHandler;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiElementVisitor;

/** IntelliJ-compat stub for SSR global matching visitor. */
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

    /** IntelliJ-compat stub for MatchContext used by SSR. */
    public static class MatchContext {
        public Pattern getPattern() { return new Pattern(); }
    }

    /** IntelliJ-compat stub for Pattern holding MatchingHandlers. */
    public static class Pattern {
        public MatchingHandler getHandler(PsiElement element) { return null; }
    }
}
