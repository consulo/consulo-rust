/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring;

import consulo.language.editor.refactoring.RefactoringSupportProvider;
import consulo.language.psi.PsiElement;
import consulo.language.editor.refactoring.action.RefactoringActionHandler;
import consulo.language.editor.refactoring.changeSignature.ChangeSignatureHandler;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.ide.refactoring.changeSignature.RsChangeSignatureHandler;
import org.rust.ide.refactoring.extractFunction.RsExtractFunctionHandler;
import org.rust.ide.refactoring.introduceConstant.RsIntroduceConstantHandler;
import org.rust.ide.refactoring.introduceParameter.RsIntroduceParameterHandler;
import org.rust.ide.refactoring.introduceVariable.RsIntroduceVariableHandler;
import org.rust.lang.core.macros.RsExpandedElementUtil;
import org.rust.lang.core.psi.ext.RsNameIdentifierOwner;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import org.rust.lang.RsLanguage;

@ExtensionImpl
public class RsRefactoringSupportProvider extends RefactoringSupportProvider {
    @jakarta.annotation.Nonnull @Override public consulo.language.Language getLanguage() { return org.rust.lang.RsLanguage.INSTANCE; }


    @Override
    public boolean isMemberInplaceRenameAvailable(@Nonnull PsiElement element, @Nullable PsiElement context) {
        return element instanceof RsNameIdentifierOwner && !RsExpandedElementUtil.isExpandedFromMacro(element);
    }

    @Nonnull
    @Override
    public RefactoringActionHandler getIntroduceVariableHandler() {
        return new RsIntroduceVariableHandler();
    }

    @Nonnull
    public RefactoringActionHandler getIntroduceVariableHandler(@Nullable PsiElement element) {
        return new RsIntroduceVariableHandler();
    }

    @Nonnull
    @Override
    public RefactoringActionHandler getIntroduceConstantHandler() {
        return new RsIntroduceConstantHandler();
    }

    @Nonnull
    @Override
    public RefactoringActionHandler getExtractMethodHandler() {
        return new RsExtractFunctionHandler();
    }

    @Nonnull
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

    @Nonnull
    @Override
    public ChangeSignatureHandler getChangeSignatureHandler() {
        return new RsChangeSignatureHandler();
    }
}
