package com.intellij.codeInsight.hints;

/** IntelliJ-compat stub. */
public final class InlayInfo {
    public final String text;
    public final int offset;
    public final boolean showBefore;
    public InlayInfo(String text, int offset) { this(text, offset, true); }
    public InlayInfo(String text, int offset, boolean showBefore) {
        this.text = text;
        this.offset = offset;
        this.showBefore = showBefore;
    }
}
