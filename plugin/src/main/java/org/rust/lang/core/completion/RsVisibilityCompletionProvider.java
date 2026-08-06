/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.completion;

import consulo.language.editor.AutoPopupController;
import consulo.language.editor.completion.CompletionParameters;
import consulo.language.editor.completion.CompletionProvider;
import consulo.language.editor.completion.CompletionResultSet;
import consulo.language.editor.completion.lookup.InsertionContext;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.document.Document;
import consulo.language.util.ProcessingContext;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.completion.RsLookupElementProperties.KeywordKind;

public class RsVisibilityCompletionProvider implements CompletionProvider {
    @Override
    public void addCompletions(
        @Nonnull CompletionParameters parameters,
        @Nonnull ProcessingContext context,
        @Nonnull CompletionResultSet result
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

    private static void insertSpaceIfNeeded(@Nonnull InsertionContext ctx) {
        Document document = ctx.getDocument();
        CharSequence chars = document.getCharsSequence();
        int offset = ctx.getSelectionEndOffset();
        if (offset < chars.length() && chars.charAt(offset) == ' ') return;
        document.insertString(offset, " ");
    }
}
