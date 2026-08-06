/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.suggested;
import com.intellij.refactoring.suggested.SuggestedRefactoringExecution;
import com.intellij.refactoring.suggested.SuggestedRefactoringStateChanges;
import com.intellij.refactoring.suggested.SuggestedRefactoringUI;
import com.intellij.refactoring.suggested.SuggestedRefactoringState;
import com.intellij.refactoring.suggested.SuggestedRefactoringAvailability;
import com.intellij.refactoring.suggested.SuggestedRefactoringSupport;

import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsPatBinding;
import org.rust.lang.core.psi.RsPatIdent;
import org.rust.lang.core.psi.RsValueParameterList;
import org.rust.lang.core.psi.ext.RsNameIdentifierOwner;

public class RsSuggestedRefactoringSupport implements SuggestedRefactoringSupport {

    @Nonnull
    @Override
    public SuggestedRefactoringAvailability getAvailability() {
        return new RsSuggestedRefactoringAvailability(this);
    }

    @Nonnull
    @Override
    public SuggestedRefactoringExecution getExecution() {
        return new RsSuggestedRefactoringExecution(this);
    }

    @Nonnull
    @Override
    public SuggestedRefactoringStateChanges getStateChanges() {
        return new RsSuggestedRefactoringStateChanges(this);
    }

    @Nonnull
    @Override
    public SuggestedRefactoringUI getUi() {
        return new RsSuggestedRefactoringUI();
    }

    @Nullable
    @Override
    public TextRange importsRange(@Nonnull PsiFile psiFile) {
        return null;
    }

    @Override
    public boolean isAnchor(@Nonnull PsiElement psiElement) {
        if (psiElement instanceof RsPatBinding) {
            return psiElement.getParent() instanceof RsPatIdent
                && PsiTreeUtil.getParentOfType(psiElement, RsValueParameterList.class) == null;
        }
        if (psiElement instanceof RsNameIdentifierOwner) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isIdentifierPart(char c) {
        return Character.isUnicodeIdentifierStart(c);
    }

    @Override
    public boolean isIdentifierStart(char c) {
        return Character.isUnicodeIdentifierPart(c);
    }

    @Nullable
    @Override
    public TextRange nameRange(@Nonnull PsiElement anchor) {
        if (anchor instanceof RsNameIdentifierOwner) {
            PsiElement nameIdentifier = ((RsNameIdentifierOwner) anchor).getNameIdentifier();
            return nameIdentifier != null ? nameIdentifier.getTextRange() : null;
        }
        return null;
    }

    @Nullable
    @Override
    public TextRange signatureRange(@Nonnull PsiElement anchor) {
        if (anchor instanceof RsFunction) {
            RsFunction function = (RsFunction) anchor;
            PsiElement start = function.getIdentifier();
            PsiElement end = function.getValueParameterList() != null
                ? function.getValueParameterList().getLastChild()
                : function.getIdentifier();
            return new TextRange(start.getTextRange().getStartOffset(), end.getTextRange().getEndOffset());
        }
        if (anchor instanceof RsNameIdentifierOwner) {
            PsiElement nameIdentifier = ((RsNameIdentifierOwner) anchor).getNameIdentifier();
            return nameIdentifier != null ? nameIdentifier.getTextRange() : null;
        }
        return null;
    }
}
