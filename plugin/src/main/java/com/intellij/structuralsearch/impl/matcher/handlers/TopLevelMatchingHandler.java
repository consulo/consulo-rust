package com.intellij.structuralsearch.impl.matcher.handlers;
public class TopLevelMatchingHandler extends MatchingHandler {
    private final MatchingHandler delegate;
    public TopLevelMatchingHandler(MatchingHandler delegate) { this.delegate = delegate; }
    public MatchingHandler getDelegate() { return delegate; }
}
