package com.intellij.spellchecker.inspections;
import consulo.document.util.TextRange;
import java.util.function.Consumer;
public final class IdentifierSplitter {
    public static IdentifierSplitter getInstance() { return new IdentifierSplitter(); }
    public void split(String text, TextRange range, Consumer<TextRange> consumer) {}
}
