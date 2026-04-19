/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.suggested;

import com.intellij.refactoring.suggested.SignatureChangePresentationModel.TextFragment.Leaf;
import com.intellij.refactoring.suggested.SignaturePresentationBuilder;
import com.intellij.refactoring.suggested.SuggestedRefactoringSupport;
import org.jetbrains.annotations.NotNull;

public class RsSignaturePresentationBuilder extends SignaturePresentationBuilder {

    public RsSignaturePresentationBuilder(
        @NotNull SuggestedRefactoringSupport.Signature signature,
        @NotNull SuggestedRefactoringSupport.Signature otherSignature,
        boolean isOldSignature
    ) {
        super(signature, otherSignature, isOldSignature);
    }

    @Override
    public void buildPresentation() {
        SuggestedRefactoringSupport.SignatureAdditionalData additionalData = getSignature().getAdditionalData();
        if (!(additionalData instanceof RsSignatureAdditionalData)) return;
        RsSignatureAdditionalData rsAdditionalData = (RsSignatureAdditionalData) additionalData;

        String name = getSignature().getName();
        getFragments().add(new Leaf(name, effect(getSignature().getName(), getOtherSignature().getName())));

        if (rsAdditionalData.isFunction()) {
            buildParameterList((fragments, parameter, correspondingParameter) -> {
                fragments.add(leaf(parameter.getName(), correspondingParameter != null ? correspondingParameter.getName() : parameter.getName()));
                fragments.add(new Leaf(": "));
                fragments.add(leaf(parameter.getType(), correspondingParameter != null ? correspondingParameter.getType() : parameter.getType()));
                return kotlin.Unit.INSTANCE;
            });
        }
    }
}
