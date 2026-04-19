/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.suggested;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.suggested.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsPatBinding;
import org.rust.lang.core.psi.RsPatIdent;
import org.rust.lang.core.psi.RsValueParameterList;
import org.rust.lang.core.psi.ext.RsNameIdentifierOwner;

public class RsSuggestedRefactoringSupport implements SuggestedRefactoringSupport {

    @NotNull
    @Override
    public SuggestedRefactoringAvailability getAvailability() {
        return new RsSuggestedRefactoringAvailability(this);
    }

    @NotNull
    @Override
    public SuggestedRefactoringExecution getExecution() {
        return new RsSuggestedRefactoringExecution(this);
    }

    @NotNull
    @Override
    public SuggestedRefactoringStateChanges getStateChanges() {
        return new RsSuggestedRefactoringStateChanges(this);
    }

    @NotNull
    @Override
    public SuggestedRefactoringUI getUi() {
        return new RsSuggestedRefactoringUI();
    }

    @Nullable
    @Override
    public TextRange importsRange(@NotNull PsiFile psiFile) {
        return null;
    }

    @Override
    public boolean isAnchor(@NotNull PsiElement psiElement) {
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
    public TextRange nameRange(@NotNull PsiElement anchor) {
        if (anchor instanceof RsNameIdentifierOwner) {
            PsiElement nameIdentifier = ((RsNameIdentifierOwner) anchor).getNameIdentifier();
            return nameIdentifier != null ? nameIdentifier.getTextRange() : null;
        }
        return null;
    }

    @Nullable
    @Override
    public TextRange signatureRange(@NotNull PsiElement anchor) {
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
