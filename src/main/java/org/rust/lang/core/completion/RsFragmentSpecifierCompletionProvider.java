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
import org.rust.lang.core.macros.decl.FragmentKind;
import org.rust.lang.core.psi.RsMacroBinding;

public class RsFragmentSpecifierCompletionProvider extends RsCompletionProvider {
    public static final RsFragmentSpecifierCompletionProvider INSTANCE = new RsFragmentSpecifierCompletionProvider();

    private RsFragmentSpecifierCompletionProvider() {
    }

    @NotNull
    @Override
    public ElementPattern<PsiElement> getElementPattern() {
        return PlatformPatterns.psiElement().withParent(RsMacroBinding.class);
    }

    @Override
    protected void addCompletions(
        @NotNull CompletionParameters parameters,
        @NotNull ProcessingContext context,
        @NotNull CompletionResultSet result
    ) {
        for (String kind : FragmentKind.kinds) {
            result.addElement(LookupElements.toKeywordElement(LookupElementBuilder.create(kind).bold()));
        }
    }
}
