package com.intellij.structuralsearch.impl.matcher.compiler;

import com.intellij.structuralsearch.impl.matcher.handlers.MatchingHandler;
import consulo.language.psi.PsiElement;

/** Compiles structural-search patterns, binding matching handlers to pattern PSI elements. */
public class GlobalCompilingVisitor {
    public CompileContext getContext() { return new CompileContext(); }
    public void handle(PsiElement element) {}

    public static class CompileContext {
        public Pattern getPattern() { return new Pattern(); }
    }

    public static class Pattern {
        public void setHandler(PsiElement element, MatchingHandler handler) {}
        public MatchingHandler getHandler(PsiElement element) { return null; }
    }
}
