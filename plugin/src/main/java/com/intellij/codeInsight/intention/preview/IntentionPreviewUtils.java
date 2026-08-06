package com.intellij.codeInsight.intention.preview;

import consulo.application.WriteAction;

/** IntelliJ-compat stub: intention-preview helpers. Consulo runs the runnable under a write action. */
public final class IntentionPreviewUtils {
    private IntentionPreviewUtils() {}

    public static void write(Runnable action) {
        WriteAction.run(action::run);
    }

    public static boolean isIntentionPreviewActive() {
        return false;
    }

    public static boolean isPreviewElement(consulo.language.psi.PsiElement element) {
        return false;
    }

    public static <T> T writeAndCompute(java.util.function.Supplier<T> action) {
        return WriteAction.compute(action::get);
    }
}
