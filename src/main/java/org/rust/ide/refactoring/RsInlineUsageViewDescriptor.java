/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring;

import com.intellij.openapi.util.NlsContexts.ListItem;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.usageView.UsageViewBundle;
import com.intellij.usageView.UsageViewDescriptor;
import org.jetbrains.annotations.NotNull;

public class RsInlineUsageViewDescriptor implements UsageViewDescriptor {
    @NotNull
    private final PsiElement myElement;
    @NotNull
    @ListItem
    private final String myHeader;

    public RsInlineUsageViewDescriptor(@NotNull PsiElement element, @NotNull @ListItem String header) {
        myElement = element;
        myHeader = header;
    }

    @NotNull
    public PsiElement getElement() {
        return myElement;
    }

    @NotNull
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
    @NotNull
    public PsiElement[] getElements() {
        return new PsiElement[]{myElement};
    }

    @Override
    public String getProcessedElementsHeader() {
        return myHeader;
    }
}
