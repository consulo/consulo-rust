/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring;

import com.intellij.lang.refactoring.RefactoringSupportProvider;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.RefactoringActionHandler;
import com.intellij.refactoring.changeSignature.ChangeSignatureHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.refactoring.changeSignature.RsChangeSignatureHandler;
import org.rust.ide.refactoring.extractFunction.RsExtractFunctionHandler;
import org.rust.ide.refactoring.introduceConstant.RsIntroduceConstantHandler;
import org.rust.ide.refactoring.introduceParameter.RsIntroduceParameterHandler;
import org.rust.ide.refactoring.introduceVariable.RsIntroduceVariableHandler;
import org.rust.lang.core.macros.RsExpandedElementUtil;
import org.rust.lang.core.psi.ext.RsNameIdentifierOwner;

public class RsRefactoringSupportProvider extends RefactoringSupportProvider {

    @Override
    public boolean isMemberInplaceRenameAvailable(@NotNull PsiElement element, @Nullable PsiElement context) {
        return element instanceof RsNameIdentifierOwner && !RsExpandedElementUtil.isExpandedFromMacro(element);
    }

    @NotNull
    @Override
    public RefactoringActionHandler getIntroduceVariableHandler() {
        return new RsIntroduceVariableHandler();
    }

    @NotNull
    @Override
    public RefactoringActionHandler getIntroduceVariableHandler(@Nullable PsiElement element) {
        return new RsIntroduceVariableHandler();
    }

    @NotNull
    @Override
    public RefactoringActionHandler getIntroduceConstantHandler() {
        return new RsIntroduceConstantHandler();
    }

    @NotNull
    @Override
    public RefactoringActionHandler getExtractMethodHandler() {
        return new RsExtractFunctionHandler();
    }

    @NotNull
    @Override
    public RefactoringActionHandler getIntroduceParameterHandler() {
        return new RsIntroduceParameterHandler();
    }

    /**
     * Handled by {@link org.rust.ide.refactoring.extractTrait.RsExtractTraitAction}, which is needed to change action text
     */
    @Nullable
    @Override
    public RefactoringActionHandler getExtractInterfaceHandler() {
        return null;
    }

    @NotNull
    @Override
    public ChangeSignatureHandler getChangeSignatureHandler() {
        return new RsChangeSignatureHandler();
    }
}
