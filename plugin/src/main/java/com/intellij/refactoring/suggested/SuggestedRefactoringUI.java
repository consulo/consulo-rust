package com.intellij.refactoring.suggested;

import consulo.language.psi.PsiCodeFragment;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.List;

/** IntelliJ-compat stub. */
public abstract class SuggestedRefactoringUI {
    @Nonnull
    public SignatureChangePresentationModel buildSignatureChangePresentation(
        @Nonnull SuggestedRefactoringSupport.Signature oldSignature,
        @Nonnull SuggestedRefactoringSupport.Signature newSignature) {
        return new SignatureChangePresentationModel();
    }

    @Nonnull
    public SignaturePresentationBuilder createSignaturePresentationBuilder(
        @Nonnull SuggestedRefactoringSupport.Signature signature,
        @Nonnull SuggestedRefactoringSupport.Signature otherSignature,
        boolean isOldSignature) {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    public List<NewParameterData> extractNewParameterData(@Nonnull SuggestedChangeSignatureData data) {
        return Collections.emptyList();
    }

    @Nullable
    public SuggestedRefactoringExecution.NewParameterValue extractValue(@Nonnull PsiCodeFragment fragment) {
        return null;
    }

    public static class NewParameterData {
        public final String name;
        public final Object valueFragment;
        public final boolean useAnySingleVariable;
        public final String placeholder;
        public final Object additionalData;
        public final boolean offerAnyVar;
        public NewParameterData(String name, Object valueFragment, boolean useAnySingleVariable,
                                @Nullable String placeholder, @Nullable Object additionalData, boolean offerAnyVar) {
            this.name = name;
            this.valueFragment = valueFragment;
            this.useAnySingleVariable = useAnySingleVariable;
            this.placeholder = placeholder;
            this.additionalData = additionalData;
            this.offerAnyVar = offerAnyVar;
        }
    }
}
