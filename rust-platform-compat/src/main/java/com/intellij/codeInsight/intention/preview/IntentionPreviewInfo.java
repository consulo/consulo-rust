package com.intellij.codeInsight.intention.preview;
/** Result of computing an intention preview; only the empty no-preview value exists. */
public interface IntentionPreviewInfo {
    IntentionPreviewInfo EMPTY = new IntentionPreviewInfo() {};
}
