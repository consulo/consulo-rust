/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.suggested;

import com.intellij.psi.PsiCodeFragment;
import com.intellij.refactoring.suggested.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.utils.imports.ImportUtils;
import org.rust.lang.core.psi.RsExpressionCodeFragment;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsMod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RsSuggestedRefactoringUI extends SuggestedRefactoringUI {

    @NotNull
    @Override
    public SignaturePresentationBuilder createSignaturePresentationBuilder(
        @NotNull SuggestedRefactoringSupport.Signature signature,
        @NotNull SuggestedRefactoringSupport.Signature otherSignature,
        boolean isOldSignature
    ) {
        return new RsSignaturePresentationBuilder(signature, otherSignature, isOldSignature);
    }

    @NotNull
    @Override
    public List<NewParameterData> extractNewParameterData(@NotNull SuggestedChangeSignatureData data) {
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
    public SuggestedRefactoringExecution.NewParameterValue.Expression extractValue(@NotNull PsiCodeFragment fragment) {
        if (!(fragment instanceof RsExpressionCodeFragment)) return null;
        RsExpressionCodeFragment rsFragment = (RsExpressionCodeFragment) fragment;
        if (rsFragment.getExpr() == null) return null;
        return new SuggestedRefactoringExecution.NewParameterValue.Expression(rsFragment.getExpr());
    }
}
