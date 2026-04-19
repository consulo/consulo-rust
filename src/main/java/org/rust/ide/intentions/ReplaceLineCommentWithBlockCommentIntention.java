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

import java.util.ArrayList;
import java.util.List;

public class ReplaceLineCommentWithBlockCommentIntention extends RsElementBaseIntentionAction<PsiComment> {

    private static final int LINE_COMMENT_PREFIX_LEN = 2; // the length of //

    @NotNull
    @Override
    public String getText() {
        return getFamilyName();
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return RsBundle.message("intention.family.name.replace.with.block.comment");
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
        if (comment.getTokenType() != RustParserDefinition.EOL_COMMENT) return null;

        // Find the first comment in the chain
        PsiComment first = comment;
        PsiComment prev = getPrevComment(first);
        while (prev != null) {
            first = prev;
            prev = getPrevComment(first);
        }
        if (!PsiModificationUtil.canReplace(first)) return null;
        return first;
    }

    @Override
    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiComment ctx) {
        PsiComment firstLineComment = ctx;
        String indent = "";
        PsiElement prevSibling = firstLineComment.getPrevSibling();
        if (prevSibling instanceof PsiWhiteSpace) {
            String wsText = prevSibling.getText();
            int lastNewline = wsText.lastIndexOf('\n');
            if (lastNewline >= 0) {
                indent = wsText.substring(lastNewline + 1);
            }
        }

        List<PsiComment> lineComments = new ArrayList<>();
        lineComments.add(firstLineComment);
        PsiComment next = getNextComment(firstLineComment);
        while (next != null) {
            lineComments.add(next);
            next = getNextComment(next);
        }

        String blockCommentText;
        if (lineComments.size() == 1) {
            blockCommentText = " " + getContent(firstLineComment) + " ";
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("\n");
            for (PsiComment lc : lineComments) {
                sb.append(indent).append(getContent(lc)).append("\n");
            }
            sb.append(indent);
            blockCommentText = sb.toString();
        }

        for (int i = 1; i < lineComments.size(); i++) {
            PsiComment lc = lineComments.get(i);
            PsiElement prev2 = lc.getPrevSibling();
            if (prev2 instanceof PsiWhiteSpace) {
                prev2.delete();
            }
            lc.delete();
        }

        PsiElement newBlockComment = new RsPsiFactory(project).createBlockComment(blockCommentText);
        firstLineComment.replace(newBlockComment);
    }

    @NotNull
    private String getContent(@NotNull PsiComment comment) {
        String text = comment.getText();
        String stripped = text.substring(LINE_COMMENT_PREFIX_LEN);
        return stripped.trim()
            .replace("/*", "/ *")
            .replace("*/", "* /");
    }

    @Nullable
    private PsiComment getPrevComment(@NotNull PsiComment comment) {
        PsiElement prev = PsiElementExt.getPrevNonWhitespaceSibling(comment);
        if (prev instanceof PsiComment && ((PsiComment) prev).getTokenType() == RustParserDefinition.EOL_COMMENT) {
            return (PsiComment) prev;
        }
        return null;
    }

    @Nullable
    private PsiComment getNextComment(@NotNull PsiComment comment) {
        PsiElement next = PsiElementExt.getNextNonWhitespaceSibling(comment);
        if (next instanceof PsiComment && ((PsiComment) next).getTokenType() == RustParserDefinition.EOL_COMMENT) {
            return (PsiComment) next;
        }
        return null;
    }
}
