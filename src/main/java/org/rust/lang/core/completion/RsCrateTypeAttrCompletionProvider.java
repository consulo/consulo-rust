/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.rust.ide.inspections.lints.RsUnknownCrateTypesInspection;
import org.rust.lang.core.RsPsiPattern;
import org.rust.lang.core.psi.RsElementTypes;

import java.util.stream.Collectors;

public class RsCrateTypeAttrCompletionProvider extends RsCompletionProvider {
    public static final RsCrateTypeAttrCompletionProvider INSTANCE = new RsCrateTypeAttrCompletionProvider();

    private RsCrateTypeAttrCompletionProvider() {
    }

    @Override
    protected void addCompletions(
        @NotNull CompletionParameters parameters,
        @NotNull ProcessingContext context,
        @NotNull CompletionResultSet result
    ) {
        result.addAllElements(
            RsUnknownCrateTypesInspection.KNOWN_CRATE_TYPES.stream()
                .map(LookupElementBuilder::create)
                .collect(Collectors.toList())
        );
    }

    @NotNull
    @Override
    public ElementPattern<? extends PsiElement> getElementPattern() {
        return PlatformPatterns.psiElement(RsElementTypes.STRING_LITERAL)
            .withParent(RsPsiPattern.insideCrateTypeAttrValue);
    }
}
