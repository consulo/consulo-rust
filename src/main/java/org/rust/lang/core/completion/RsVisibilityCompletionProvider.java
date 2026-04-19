/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.editor.Document;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.completion.RsLookupElementProperties.KeywordKind;

public class RsVisibilityCompletionProvider extends CompletionProvider<CompletionParameters> {
    @Override
    protected void addCompletions(
        @NotNull CompletionParameters parameters,
        @NotNull ProcessingContext context,
        @NotNull CompletionResultSet result
    ) {
        String[][] items = {
            {"pub", KeywordKind.PUB.name()},
            {"pub(crate)", KeywordKind.PUB_CRATE.name()},
            {"pub(super)", KeywordKind.PUB_CRATE.name()},
        };
        for (String[] item : items) {
            String name = item[0];
            KeywordKind priority = KeywordKind.valueOf(item[1]);
            result.addElement(
                LookupElements.toKeywordElement(
                    LookupElementBuilder.create(name)
                        .bold()
                        .withInsertHandler((ctx, lookupItem) -> {
                            insertSpaceIfNeeded(ctx);
                            ctx.getEditor().getCaretModel().moveToOffset(ctx.getSelectionEndOffset());
                        }),
                    priority
                )
            );
        }
        result.addElement(
            LookupElements.toKeywordElement(
                LookupElementBuilder.create("pub()")
                    .bold()
                    .withInsertHandler((ctx, lookupItem) -> {
                        int offset = ctx.getSelectionEndOffset();
                        insertSpaceIfNeeded(ctx);
                        ctx.getEditor().getCaretModel().moveToOffset(offset - 1);
                        AutoPopupController.getInstance(ctx.getProject()).scheduleAutoPopup(ctx.getEditor());
                    }),
                KeywordKind.PUB_PARENS
            )
        );
    }

    private static void insertSpaceIfNeeded(@NotNull InsertionContext ctx) {
        Document document = ctx.getDocument();
        CharSequence chars = document.getCharsSequence();
        int offset = ctx.getSelectionEndOffset();
        if (offset < chars.length() && chars.charAt(offset) == ' ') return;
        document.insertString(offset, " ");
    }
}
