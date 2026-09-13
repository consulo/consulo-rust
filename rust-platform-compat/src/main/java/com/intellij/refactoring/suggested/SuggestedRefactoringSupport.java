package com.intellij.refactoring.suggested;

import consulo.document.util.TextRange;
import consulo.language.Language;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/** Suggested-refactoring framework hooks; every member is inert. */
public interface SuggestedRefactoringSupport {

    default Language getLanguage() { return null; }
    default boolean isAnchor(@Nonnull PsiElement psiElement) { return false; }
    default Object getSignaturePresentation() { return null; }
    default List<Object> suggestFeatures() { return List.of(); }
    default RefactoringSupport getRefactoringSupport() { return null; }
    @Nonnull default SuggestedRefactoringAvailability getAvailability() { return null; }
    @Nonnull default SuggestedRefactoringExecution getExecution() { return null; }
    @Nonnull default SuggestedRefactoringStateChanges getStateChanges() { return null; }
    @Nonnull default SuggestedRefactoringUI getUi() { return null; }
    @Nullable default TextRange importsRange(@Nonnull PsiFile psiFile) { return null; }
    @Nullable default TextRange signatureRange(@Nonnull PsiElement anchor) { return null; }
    @Nullable default SignaturePresentationBuilder createSignaturePresentationBuilder(
        @Nonnull Signature signature, @Nonnull Signature otherSignature, boolean isOldSignature) { return null; }
    @Nullable default TextRange nameRange(@Nonnull PsiElement anchor) { return null; }
    default boolean isIdentifierStart(char c) { return Character.isJavaIdentifierStart(c); }
    default boolean isIdentifierPart(char c) { return Character.isJavaIdentifierPart(c); }
    @Nonnull default SyntaxScopeProvider getSyntaxScopeProvider() { return new SyntaxScopeProvider() {}; }
    default boolean hasSyntaxError(@Nonnull PsiElement psiElement) { return false; }

    interface Signature {
        String getName();
        String getType();
        List<Parameter> getParameters();
        SignatureAdditionalData getAdditionalData();
        @Nullable default Parameter parameterById(int id) {
            for (Parameter p : getParameters()) { if (p.getId() == id) return p; }
            return null;
        }
        default int parameterIndex(Parameter p) {
            return getParameters().indexOf(p);
        }

        @Nullable static Signature create(String name, String type, List<Parameter> parameters, SignatureAdditionalData data) {
            return new Signature() {
                public String getName() { return name; }
                public String getType() { return type; }
                public List<Parameter> getParameters() { return parameters; }
                public SignatureAdditionalData getAdditionalData() { return data; }
            };
        }
    }

    class Parameter {
        private final int id;
        private final String name;
        private final String type;
        private final ParameterAdditionalData additionalData;

        public Parameter(int id, String name, String type, ParameterAdditionalData additionalData) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.additionalData = additionalData;
        }

        public Parameter(Object idObject, String name, String type, ParameterAdditionalData additionalData) {
            this(idObject != null ? idObject.hashCode() : 0, name, type, additionalData);
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public String getType() { return type; }
        public String getDefaultValue() { return null; }
        public ParameterAdditionalData getAdditionalData() { return additionalData; }
    }

    interface SignatureAdditionalData {}
    interface ParameterAdditionalData {}
    interface RefactoringAvailability {}
    interface RefactoringSupport {}
    interface SyntaxScopeProvider {}
}
