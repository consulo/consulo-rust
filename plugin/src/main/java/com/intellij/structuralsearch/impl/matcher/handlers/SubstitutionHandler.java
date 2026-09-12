package com.intellij.structuralsearch.impl.matcher.handlers;
import consulo.language.psi.PsiElement;
public class SubstitutionHandler extends MatchingHandler {
    private final String name;
    private final boolean target;
    private final int minOccurs;
    private final int maxOccurs;
    private final boolean greedy;
    public SubstitutionHandler(String name, boolean target, int minOccurs, int maxOccurs, boolean greedy) {
        this.name = name; this.target = target; this.minOccurs = minOccurs; this.maxOccurs = maxOccurs; this.greedy = greedy;
    }
    public String getName() { return name; }
    public boolean isTarget() { return target; }
    public int getMinOccurs() { return minOccurs; }
    public int getMaxOccurs() { return maxOccurs; }
    public boolean validate(PsiElement element, com.intellij.structuralsearch.impl.matcher.GlobalMatchingVisitor.MatchContext context) { return false; }
}
