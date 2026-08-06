package com.intellij.spellchecker.inspections;
import consulo.document.util.TextRange;
import java.util.function.Consumer;
public final class PlainTextSplitter {
    public static PlainTextSplitter getInstance() { return new PlainTextSplitter(); }
    public void split(String text, TextRange range, Consumer<TextRange> consumer) {}
}
