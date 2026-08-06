/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.pattern.ElementPattern;
import consulo.language.pattern.PlatformPatterns;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;
import jakarta.annotation.Nonnull;
import org.rust.ide.inspections.lints.RsUnknownCrateTypesInspection;
import org.rust.lang.core.RsPsiPattern;
import org.rust.lang.core.psi.RsElementTypes;

import java.util.stream.Collectors;

public class RsCrateTypeAttrCompletionProvider extends RsCompletionProvider {
    public static final RsCrateTypeAttrCompletionProvider INSTANCE = new RsCrateTypeAttrCompletionProvider();

    private RsCrateTypeAttrCompletionProvider() {
    }

    @Override
    public void addCompletions(
        @Nonnull CompletionParameters parameters,
        @Nonnull ProcessingContext context,
        @Nonnull CompletionResultSet result
    ) {
        result.addAllElements(
            RsUnknownCrateTypesInspection.KNOWN_CRATE_TYPES.stream()
                .map(LookupElementBuilder::create)
                .collect(Collectors.toList())
        );
    }

    @Nonnull
    @Override
    public ElementPattern<? extends PsiElement> getElementPattern() {
        return PlatformPatterns.psiElement(RsElementTypes.STRING_LITERAL)
            .withParent(RsPsiPattern.insideCrateTypeAttrValue);
    }
}
