/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.completion;

import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.pattern.ElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.RsPsiPattern;
import org.rust.lang.core.completion.RsCompletionProvider;

public class RsStaticLifetimeCompletionProvider extends RsCompletionProvider {
    public static final RsStaticLifetimeCompletionProvider INSTANCE = new RsStaticLifetimeCompletionProvider();

    private RsStaticLifetimeCompletionProvider() {
    }

    @Nonnull
    @Override
    public ElementPattern<PsiElement> getElementPattern() {
        return RsPsiPattern.lifetimeIdentifier;
    }

    @Override
    public void addCompletions(
        @Nonnull CompletionParameters parameters,
        @Nonnull ProcessingContext context,
        @Nonnull CompletionResultSet result
    ) {
        result.addElement(
            LookupElementBuilder
                .create("'static")
                .bold()
        );
    }
}
