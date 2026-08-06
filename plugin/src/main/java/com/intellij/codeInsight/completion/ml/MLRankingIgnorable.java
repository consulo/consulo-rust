package com.intellij.codeInsight.completion.ml;
/** IntelliJ-compat stub — marker for ML-ranking ignore. */
public interface MLRankingIgnorable {
    /** No-op wrapper — ML ranking isn't used in Consulo. */
    static consulo.language.editor.completion.lookup.LookupElement wrap(consulo.language.editor.completion.lookup.LookupElement element) {
        return element;
    }
}
