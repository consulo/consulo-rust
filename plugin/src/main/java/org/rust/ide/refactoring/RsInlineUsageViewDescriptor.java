/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring;


import consulo.language.psi.PsiElement;
import consulo.language.editor.refactoring.RefactoringBundle;
import consulo.usage.UsageViewBundle;
import consulo.usage.UsageViewDescriptor;
import jakarta.annotation.Nonnull;

public class RsInlineUsageViewDescriptor implements UsageViewDescriptor {
    @Nonnull
    private final PsiElement myElement;
    @Nonnull
    
    private final String myHeader;

    public RsInlineUsageViewDescriptor(@Nonnull PsiElement element, @Nonnull  String header) {
        myElement = element;
        myHeader = header;
    }

    @Nonnull
    public PsiElement getElement() {
        return myElement;
    }

    @Nonnull
    public String getHeader() {
        return myHeader;
    }

    @Override
    public String getCommentReferencesText(int usagesCount, int filesCount) {
        return RefactoringBundle.message("comments.elements.header",
            UsageViewBundle.getOccurencesString(usagesCount, filesCount));
    }

    @SuppressWarnings("InvalidBundleOrProperty")
    @Override
    public String getCodeReferencesText(int usagesCount, int filesCount) {
        return RefactoringBundle.message("invocations.to.be.inlined",
            UsageViewBundle.getReferencesString(usagesCount, filesCount));
    }

    @Override
    @Nonnull
    public PsiElement[] getElements() {
        return new PsiElement[]{myElement};
    }

    @Override
    public String getProcessedElementsHeader() {
        return myHeader;
    }
}
