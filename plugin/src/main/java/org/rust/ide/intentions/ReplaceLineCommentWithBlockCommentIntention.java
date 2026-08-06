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
import org.rust.lang.core.parser.RustParserDefinition;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.ext.PsiElementExt;

import java.util.ArrayList;
import java.util.List;

public class ReplaceLineCommentWithBlockCommentIntention extends RsElementBaseIntentionAction<PsiComment> {

    private static final int LINE_COMMENT_PREFIX_LEN = 2; // the length of //

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return getFamilyName();
        }

    @Nonnull
    public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.replace.with.block.comment"));
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
        if (((consulo.language.ast.IElementType) comment.getTokenType()) != RustParserDefinition.EOL_COMMENT) return null;

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
    public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiComment ctx) {
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

    @Nonnull
    private String getContent(@Nonnull PsiComment comment) {
        String text = comment.getText();
        String stripped = text.substring(LINE_COMMENT_PREFIX_LEN);
        return stripped.trim()
            .replace("/*", "/ *")
            .replace("*/", "* /");
    }

    @Nullable
    private PsiComment getPrevComment(@Nonnull PsiComment comment) {
        PsiElement prev = PsiElementExt.getPrevNonWhitespaceSibling(comment);
        if (prev instanceof PsiComment && ((PsiComment) prev).getTokenType() == RustParserDefinition.EOL_COMMENT) {
            return (PsiComment) prev;
        }
        return null;
    }

    @Nullable
    private PsiComment getNextComment(@Nonnull PsiComment comment) {
        PsiElement next = PsiElementExt.getNextNonWhitespaceSibling(comment);
        if (next instanceof PsiComment && ((PsiComment) next).getTokenType() == RustParserDefinition.EOL_COMMENT) {
            return (PsiComment) next;
        }
        return null;
    }
}
