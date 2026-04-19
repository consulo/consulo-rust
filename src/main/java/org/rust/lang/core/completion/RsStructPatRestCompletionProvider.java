/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsPatStruct;
import org.rust.lang.core.psi.ext.RsElement;

import static org.rust.lang.core.PsiElementPatternExtUtil.psiElement;

public class RsStructPatRestCompletionProvider extends RsCompletionProvider {
    public static final RsStructPatRestCompletionProvider INSTANCE = new RsStructPatRestCompletionProvider();

    private RsStructPatRestCompletionProvider() {
    }

    @NotNull
    @Override
    public PsiElementPattern.Capture<PsiElement> getElementPattern() {
        return PlatformPatterns
            .psiElement()
            .withSuperParent(3, psiElement(RsPatStruct.class));
    }

    @Override
    protected void addCompletions(
        @NotNull CompletionParameters parameters,
        @NotNull ProcessingContext context,
        @NotNull CompletionResultSet result
    ) {
        RsPatStruct pat = RsElementUtil.ancestorStrict(
            Utils.safeGetOriginalOrSelf(parameters.getPosition()), RsPatStruct.class);
        if (pat == null) return;
        for (PsiElement child : pat.getChildren()) {
            if ("..".equals(child.getText())) return;
        }
        result.addElement(LookupElementBuilder.create(".."));
    }
}
