/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.util;

import com.intellij.codeInsight.completion.CodeCompletionHandlerBase;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import org.rust.lang.core.completion.RsKeywordCompletionProvider;

public class Opt {
    private final String name;
    private final ArgCompleter argCompleter;
    private final LookupElement lookupElement;

    public Opt(String name, ArgCompleter argCompleter) {
        this.name = name;
        this.argCompleter = argCompleter;
        this.lookupElement = LookupElementBuilder.create(getLong())
            .withInsertHandler((ctx, item) -> {
                if (argCompleter != null) {
                    RsKeywordCompletionProvider.addSuffix(ctx, " ");
                    ctx.setLaterRunnable(() -> {
                        CodeCompletionHandlerBase handler = new CodeCompletionHandlerBase(CompletionType.BASIC);
                        handler.invokeCompletion(ctx.getProject(), ctx.getEditor());
                    });
                }
            });
    }

    public Opt(String name) {
        this(name, null);
    }

    public String getName() {
        return name;
    }

    public String getLong() {
        return "--" + name;
    }

    public ArgCompleter getArgCompleter() {
        return argCompleter;
    }

    public LookupElement getLookupElement() {
        return lookupElement;
    }
}
