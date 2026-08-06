/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.pattern.ElementPattern;
import consulo.language.psi.PsiElement;
import consulo.language.util.ProcessingContext;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.RsPsiPattern;
import org.rust.toml.completion.RsCfgFeatureCompletionProvider.RustStringLiteralInsertionHandler;

public class RsCfgPanicCompletionProvider extends RsCompletionProvider {
    public static final RsCfgPanicCompletionProvider INSTANCE = new RsCfgPanicCompletionProvider();

    private RsCfgPanicCompletionProvider() {
    }

    @Nonnull
    @Override
    public ElementPattern<? extends PsiElement> getElementPattern() {
        return RsPsiPattern.INSTANCE.insideAnyCfgFlagValue("panic");
    }

    @Override
    public void addCompletions(
        @Nonnull CompletionParameters parameters,
        @Nonnull ProcessingContext context,
        @Nonnull CompletionResultSet result
    ) {
        for (String value : new String[]{"abort", "unwind"}) {
            result.addElement(LookupElementBuilder.create(value).withInsertHandler(new RustStringLiteralInsertionHandler()));
        }
    }
}
