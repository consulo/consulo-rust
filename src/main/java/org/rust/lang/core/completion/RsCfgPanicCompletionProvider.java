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
import org.rust.toml.completion.RsCfgFeatureCompletionProvider.RustStringLiteralInsertionHandler;

public class RsCfgPanicCompletionProvider extends RsCompletionProvider {
    public static final RsCfgPanicCompletionProvider INSTANCE = new RsCfgPanicCompletionProvider();

    private RsCfgPanicCompletionProvider() {
    }

    @NotNull
    @Override
    public ElementPattern<? extends PsiElement> getElementPattern() {
        return RsPsiPattern.INSTANCE.insideAnyCfgFlagValue("panic");
    }

    @Override
    protected void addCompletions(
        @NotNull CompletionParameters parameters,
        @NotNull ProcessingContext context,
        @NotNull CompletionResultSet result
    ) {
        for (String value : new String[]{"abort", "unwind"}) {
            result.addElement(LookupElementBuilder.create(value).withInsertHandler(new RustStringLiteralInsertionHandler()));
        }
    }
}
