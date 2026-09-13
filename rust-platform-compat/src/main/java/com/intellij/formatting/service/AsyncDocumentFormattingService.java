package com.intellij.formatting.service;

import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public abstract class AsyncDocumentFormattingService implements FormattingService {
    @Override public boolean canFormat(@Nonnull PsiFile file) { return false; }
    public abstract static class FormattingTask implements Runnable {
        public boolean isRunUnderProgress() { return false; }
        public boolean cancel() { return false; }
    }
    @Nullable protected abstract FormattingTask createFormattingTask(@Nonnull AsyncFormattingRequest request);
    @Nonnull protected abstract String getNotificationGroupId();
    @Nonnull protected abstract String getName();

    public FormattingReason getFormattingReason() { return FormattingReason.ReformatCode; }

    public enum FormattingReason {
        ReformatCode, ReformatCodeBeforeCommit, Implicit
    }
}
