/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.pattern.ElementPattern;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;
import jakarta.annotation.Nonnull;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.macros.MacroExpansionUtil;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.openapiext.Testmark;

import java.util.List;

/**
 * Provides completion inside a macro argument (e.g. {@code foo!(caret)}) if the macro IS expanded
 * successfully. If macro is not expanded successfully,
 * {@link RsPartialMacroArgumentCompletionProvider} is used.
 */
public class RsFullMacroArgumentCompletionProvider extends RsCompletionProvider {
    public static final RsFullMacroArgumentCompletionProvider INSTANCE = new RsFullMacroArgumentCompletionProvider();

    private RsFullMacroArgumentCompletionProvider() {
    }

    @Override
    public void addCompletions(
        @Nonnull CompletionParameters parameters,
        @Nonnull ProcessingContext context,
        @Nonnull CompletionResultSet result
    ) {
        PsiElement position = parameters.getPosition();
        List<PsiElement> expansionElements = MacroExpansionUtil.findExpansionElements(position);
        if (expansionElements == null || expansionElements.isEmpty()) return;
        PsiElement dstElement = expansionElements.get(0);
        int dstOffset = dstElement.getTextRange().getStartOffset() + (parameters.getOffset() - position.getTextRange().getStartOffset());
        Testmarks.Touched.hit();
        Utils.rerunCompletion(parameters.withPosition(dstElement, dstOffset), result);
    }

    @Nonnull
    @Override
    public ElementPattern<PsiElement> getElementPattern() {
        return PlatformPatterns.psiElement()
            .withLanguage(RsLanguage.INSTANCE)
            .inside(PlatformPatterns.psiElement(RsElementTypes.MACRO_ARGUMENT));
    }

    public static final class Testmarks {
        public static final Testmark Touched = new Testmark();
    }
}
