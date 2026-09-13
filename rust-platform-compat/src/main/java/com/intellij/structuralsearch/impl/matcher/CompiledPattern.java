package com.intellij.structuralsearch.impl.matcher;

import com.intellij.structuralsearch.impl.matcher.strategies.MatchingStrategy;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;

/** Compiled structural-search pattern. */
public abstract class CompiledPattern {
    public void setStrategy(MatchingStrategy strategy) {}
    public NodeIterator getNodes() { return new NodeIterator(); }
    public String getTypedVarString(@Nonnull PsiElement element) { return element.getText(); }

    public static class NodeIterator {
        public boolean hasNext() { return false; }
        public PsiElement current() { return null; }
        public void advance() {}
        public void reset() {}
    }
}
