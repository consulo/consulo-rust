/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import org.rust.lang.core.psi.ext.RsElementUtil;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.pattern.PsiElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsPatStruct;
import org.rust.lang.core.psi.ext.RsElement;

import static org.rust.lang.core.PsiElementPatternExtUtil.psiElement;

public class RsStructPatRestCompletionProvider extends RsCompletionProvider {
    public static final RsStructPatRestCompletionProvider INSTANCE = new RsStructPatRestCompletionProvider();

    private RsStructPatRestCompletionProvider() {
    }

    @Nonnull
    @Override
    public PsiElementPattern.Capture<PsiElement> getElementPattern() {
        return PlatformPatterns
            .psiElement()
            .withSuperParent(3, psiElement(RsPatStruct.class));
    }

    @Override
    public void addCompletions(
        @Nonnull CompletionParameters parameters,
        @Nonnull ProcessingContext context,
        @Nonnull CompletionResultSet result
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
