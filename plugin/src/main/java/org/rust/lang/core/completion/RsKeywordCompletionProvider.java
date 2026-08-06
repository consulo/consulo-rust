/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import org.rust.lang.core.psi.ext.RsElementUtil;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.codeEditor.util.EditorModificationUtil;
import consulo.language.util.ProcessingContext;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsUnitType;
import org.rust.lang.core.psi.ext.RsElement;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class RsKeywordCompletionProvider implements CompletionProvider {
    private final String[] myKeywords;

    public RsKeywordCompletionProvider(String... keywords) {
        myKeywords = keywords;
    }

    @Override
    public void addCompletions(
        @Nonnull CompletionParameters parameters,
        @Nonnull ProcessingContext context,
        @Nonnull CompletionResultSet result
    ) {
        for (String keyword : myKeywords) {
            LookupElementBuilder builder = LookupElementBuilder.create(keyword).bold();
            builder = addInsertionHandler(keyword, builder, parameters);
            result.addElement(LookupElements.toKeywordElement(builder));
        }
    }

    public static void addSuffix(@Nonnull InsertionContext ctx, @Nonnull String suffix) {
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
