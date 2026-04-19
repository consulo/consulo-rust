/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.ElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.RsPsiPattern;

public class RsStaticLifetimeCompletionProvider extends RsCompletionProvider {
    public static final RsStaticLifetimeCompletionProvider INSTANCE = new RsStaticLifetimeCompletionProvider();

    private RsStaticLifetimeCompletionProvider() {
    }

    @NotNull
    @Override
    public ElementPattern<PsiElement> getElementPattern() {
        return RsPsiPattern.lifetimeIdentifier;
    }

    @Override
    protected void addCompletions(
        @NotNull CompletionParameters parameters,
        @NotNull ProcessingContext context,
        @NotNull CompletionResultSet result
    ) {
        result.addElement(
            LookupElementBuilder
                .create("'static")
                .bold()
        );
    }
}
