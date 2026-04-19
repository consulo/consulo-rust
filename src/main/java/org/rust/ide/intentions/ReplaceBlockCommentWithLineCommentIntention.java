/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.intentions.util.macros.InvokeInside;
import org.rust.ide.utils.PsiModificationUtil;
import org.rust.lang.core.parser.RustParserDefinition;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.ext.PsiElementExt;

public class ReplaceBlockCommentWithLineCommentIntention extends RsElementBaseIntentionAction<PsiComment> {

    private static final int BLOCK_COMMENT_DELIMITER_LEN = 2; // the length of /* or */

    @NotNull
    @Override
    public String getText() {
        return getFamilyName();
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.replace.with.end.line.comment");
    }

    @NotNull
    @Override
    public InvokeInside getAttributeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    @NotNull
    @Override
    public InvokeInside getFunctionLikeMacroHandlingStrategy() {
        return InvokeInside.MACRO_CALL;
    }

    @Nullable
    @Override
    public PsiComment findApplicableContext(@NotNull Project project, @NotNull Editor editor, @NotNull PsiElement element) {
        PsiComment comment = PsiElementExt.ancestorOrSelf(element, PsiComment.class);
        if (comment == null) return null;
        if (comment.getTokenType() != RustParserDefinition.BLOCK_COMMENT) return null;
        if (!PsiModificationUtil.canReplace(comment)) return null;
        return comment;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiComment ctx) {
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

    @NotNull
    private String getContent(@NotNull PsiComment comment) {
        String text = comment.getText();
        String stripped = text.substring(BLOCK_COMMENT_DELIMITER_LEN, text.length() - BLOCK_COMMENT_DELIMITER_LEN);
        return stripped.trim();
    }
}
