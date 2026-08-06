package com.intellij.refactoring.suggested;
import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/** IntelliJ-compat stub — builder for signature-change presentation. */
public abstract class SignaturePresentationBuilder {
    protected final SuggestedRefactoringSupport.Signature signature;
    protected final SuggestedRefactoringSupport.Signature otherSignature;
    protected final boolean isOldSignature;
    protected final List<SignatureChangePresentationModel.TextFragment> fragments = new ArrayList<>();

    public SignaturePresentationBuilder(@Nonnull SuggestedRefactoringSupport.Signature signature,
                                         @Nonnull SuggestedRefactoringSupport.Signature otherSignature,
                                         boolean isOldSignature) {
        this.signature = signature;
        this.otherSignature = otherSignature;
        this.isOldSignature = isOldSignature;
    }
    public abstract void buildPresentation();
    public SuggestedRefactoringSupport.Signature getSignature() { return signature; }
    public SuggestedRefactoringSupport.Signature getOtherSignature() { return otherSignature; }
    public List<SignatureChangePresentationModel.TextFragment> getFragments() { return fragments; }
    protected SignatureChangePresentationModel.Effect effect(String a, String b) { return null; }
    protected SignatureChangePresentationModel.TextFragment leaf(String a, String b) { return new SignatureChangePresentationModel.TextFragment.Leaf(a); }
    protected void buildParameterList(ParameterFragmentBuilder fragmentBuilder) {}

    @FunctionalInterface
    public interface ParameterFragmentBuilder {
        Object build(List<SignatureChangePresentationModel.TextFragment> fragments,
                     SuggestedRefactoringSupport.Parameter param,
                     SuggestedRefactoringSupport.Parameter correspondingParam);
    }
}
