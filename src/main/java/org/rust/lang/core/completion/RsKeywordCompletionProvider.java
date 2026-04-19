/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsUnitType;
import org.rust.lang.core.psi.ext.RsElement;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class RsKeywordCompletionProvider extends CompletionProvider<CompletionParameters> {
    private final String[] myKeywords;

    public RsKeywordCompletionProvider(String... keywords) {
        myKeywords = keywords;
    }

    @Override
    protected void addCompletions(
        @NotNull CompletionParameters parameters,
        @NotNull ProcessingContext context,
        @NotNull CompletionResultSet result
    ) {
        for (String keyword : myKeywords) {
            LookupElementBuilder builder = LookupElementBuilder.create(keyword).bold();
            builder = addInsertionHandler(keyword, builder, parameters);
            result.addElement(LookupElements.toKeywordElement(builder));
        }
    }

    public static void addSuffix(@NotNull InsertionContext ctx, @NotNull String suffix) {
        ctx.getDocument().insertString(ctx.getSelectionEndOffset(), suffix);
        EditorModificationUtil.moveCaretRelatively(ctx.getEditor(), suffix.length());
    }

    private static final Set<String> ALWAYS_NEEDS_SPACE = new HashSet<>(Arrays.asList(
        "as", "crate", "const", "async", "enum", "extern", "fn", "for", "impl", "let", "mod", "mut",
        "static", "struct", "trait", "type", "union", "unsafe", "use", "where"
    ));

    private static LookupElementBuilder addInsertionHandler(
        String keyword,
        LookupElementBuilder builder,
        CompletionParameters parameters
    ) {
        String suffix;
        if (ALWAYS_NEEDS_SPACE.contains(keyword)) {
            suffix = " ";
        } else if ("return".equals(keyword)) {
            RsFunction fn = RsElementUtil.ancestorStrict(parameters.getPosition(), RsFunction.class);
            if (fn == null) return builder;
            boolean returnsUnit = fn.getRetType() == null || fn.getRetType().getTypeReference() instanceof RsUnitType;
            suffix = returnsUnit ? ";" : " ";
        } else {
            return builder;
        }

        return builder.withInsertHandler((ctx, item) -> addSuffix(ctx, suffix));
    }
}
