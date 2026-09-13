/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.suggested;
import com.intellij.refactoring.suggested.SuggestedRefactoringUI;
import com.intellij.refactoring.suggested.SuggestedRefactoringState;
import com.intellij.refactoring.suggested.SuggestedRefactoringAvailability;
import com.intellij.refactoring.suggested.SuggestedRefactoringSupport;
import com.intellij.refactoring.suggested.SuggestedChangeSignatureData;
import com.intellij.refactoring.suggested.SignaturePresentationBuilder;
import com.intellij.refactoring.suggested.SuggestedRefactoringExecution;

import consulo.language.psi.PsiCodeFragment;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.imports.ImportUtils;
import org.rust.lang.core.psi.impl.RsExpressionCodeFragment;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsMod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RsSuggestedRefactoringUI extends SuggestedRefactoringUI {

    @Nonnull
    @Override
    public SignaturePresentationBuilder createSignaturePresentationBuilder(
        @Nonnull SuggestedRefactoringSupport.Signature signature,
        @Nonnull SuggestedRefactoringSupport.Signature otherSignature,
        boolean isOldSignature
    ) {
        return new RsSignaturePresentationBuilder(signature, otherSignature, isOldSignature);
    }

    @Nonnull
    @Override
    public List<NewParameterData> extractNewParameterData(@Nonnull SuggestedChangeSignatureData data) {
        if (!(data.getDeclaration() instanceof RsElement)) return Collections.emptyList();
        RsElement declaration = (RsElement) data.getDeclaration();
        RsMod importContext = ImportUtils.createVirtualImportContext(declaration);

        List<NewParameterData> result = new ArrayList<>();
        for (SuggestedRefactoringSupport.Parameter parameter : data.getNewSignature().getParameters()) {
            if (data.getOldSignature().parameterById(parameter.getId()) != null) continue;
            String name = parameter.getName();
            RsExpressionCodeFragment fragment = new RsExpressionCodeFragment(
                importContext.getProject(),
                "",
                importContext,
                importContext
            );
            result.add(new NewParameterData(name, fragment, false, "", null, false));
        }
        return result;
    }

    @Nullable
    @Override
    public SuggestedRefactoringExecution.NewParameterValue.Expression extractValue(@Nonnull PsiCodeFragment fragment) {
        if (!(fragment instanceof RsExpressionCodeFragment)) return null;
        RsExpressionCodeFragment rsFragment = (RsExpressionCodeFragment) fragment;
        if (rsFragment.getExpr() == null) return null;
        return new SuggestedRefactoringExecution.NewParameterValue.Expression(rsFragment.getExpr());
    }
}
