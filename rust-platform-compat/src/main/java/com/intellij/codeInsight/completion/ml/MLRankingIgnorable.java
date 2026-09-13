package com.intellij.codeInsight.completion.ml;
import consulo.language.editor.completion.lookup.LookupElement;
/** Marker for lookup elements that must be excluded from ML-based completion ranking. */
public interface MLRankingIgnorable {
    /** No-op wrapper — ML ranking isn't used in Consulo. */
    static consulo.language.editor.completion.lookup.LookupElement wrap(consulo.language.editor.completion.lookup.LookupElement element) {
        return element;
    }
}
