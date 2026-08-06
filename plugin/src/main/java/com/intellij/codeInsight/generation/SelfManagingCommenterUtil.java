package com.intellij.codeInsight.generation;
import consulo.document.Document;
import consulo.document.util.TextRange;
/** IntelliJ-compat stub. */
public final class SelfManagingCommenterUtil {
    private SelfManagingCommenterUtil() {}
    public static TextRange getBlockCommentRange(int selectionStart, int selectionEnd, Document document, String prefix, String suffix) { return null; }
    public static void insertBlockComment(int selectionStart, int selectionEnd, Document document, String prefix, String suffix) {}
    public static void uncommentBlockComment(int startOffset, int endOffset, Document document, String prefix, String suffix) {}
}
