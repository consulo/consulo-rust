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
import org.rust.lang.core.macros.decl.FragmentKind;
import org.rust.lang.core.psi.RsMacroBinding;

public class RsFragmentSpecifierCompletionProvider extends RsCompletionProvider {
    public static final RsFragmentSpecifierCompletionProvider INSTANCE = new RsFragmentSpecifierCompletionProvider();

    private RsFragmentSpecifierCompletionProvider() {
    }

    @Nonnull
    @Override
    public ElementPattern<PsiElement> getElementPattern() {
        return PlatformPatterns.psiElement().withParent(RsMacroBinding.class);
    }

    @Override
    public void addCompletions(
        @Nonnull CompletionParameters parameters,
        @Nonnull ProcessingContext context,
        @Nonnull CompletionResultSet result
    ) {
        for (String kind : FragmentKind.kinds) {
            result.addElement(LookupElements.toKeywordElement(LookupElementBuilder.create(kind).bold()));
        }
    }
}
