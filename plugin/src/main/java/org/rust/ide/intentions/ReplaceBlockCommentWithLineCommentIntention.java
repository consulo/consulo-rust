/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiWhiteSpace;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.intentions.util.macros.InvokeInside;
import org.rust.ide.utils.PsiModificationUtil;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.ext.PsiElementExt;
import org.rust.lang.core.psi.RsTokenType;
import consulo.language.ast.IElementType;
import consulo.localize.LocalizeValue;

public class ReplaceBlockCommentWithLineCommentIntention extends RsElementBaseIntentionAction<PsiComment> {

    private static final int BLOCK_COMMENT_DELIMITER_LEN = 2; // the length of /* or */

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return getFamilyName();
        }

    @Nonnull
    public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.replace.with.end.line.comment"));
        }

    @Nonnull
    @Override
    public InvokeInside getAttributeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    @Nonnull
    @Override
    public InvokeInside getFunctionLikeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    @Nullable
    @Override
    public PsiComment findApplicableContext(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiElement element) {
        PsiComment comment = PsiElementExt.ancestorOrSelf(element, PsiComment.class);
        if (comment == null) return null;
        if (((IElementType) comment.getTokenType()) != RsTokenType.BLOCK_COMMENT) return null;
        if (!PsiModificationUtil.canReplace(comment)) return null;
        return comment;
    }

    @Override
    public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiComment ctx) {
        PsiComment blockComment = ctx;
        RsPsiFactory factory = new RsPsiFactory(project);

        String space = "";
        PsiElement prevSibling = blockComment.getPrevSibling();
        if (prevSibling instanceof PsiWhiteSpace) {
            String wsText = prevSibling.getText();
            int lastNewline = wsText.lastIndexOf('\n');
            if (lastNewline >= 0) {
                space = wsText.substring(lastNewline + 1);
            }
        }

        PsiElement indent = factory.createWhitespace("\n" + space);

        String content = getContent(blockComment);
        String[] lines = content.split("\n", -1);

        int lastIndex = lines.length - 1;
        PsiElement parent = blockComment.getParent();
        for (int i = lines.length - 1; i >= 0; i--) {
            String commentText = lines[i].trim();
            PsiElement newLineComment = factory.createLineComment(" " + commentText);
            parent.addAfter(newLineComment, blockComment);

            if (i != lastIndex) {
                parent.addAfter(indent, blockComment);
            }
        }

        blockComment.delete();
    }

    @Nonnull
    private String getContent(@Nonnull PsiComment comment) {
        String text = comment.getText();
        String stripped = text.substring(BLOCK_COMMENT_DELIMITER_LEN, text.length() - BLOCK_COMMENT_DELIMITER_LEN);
        return stripped.trim();
    }
}
