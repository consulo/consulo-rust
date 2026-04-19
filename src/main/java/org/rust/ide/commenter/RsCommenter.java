/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.commenter;

import com.intellij.codeInsight.generation.SelfManagingCommenter;
import com.intellij.codeInsight.generation.SelfManagingCommenterUtil;
import com.intellij.lang.CodeDocumentationAwareCommenter;
import com.intellij.lang.Commenter;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.parser.RustParserDefinition;
import org.rust.lang.doc.psi.RsDocKind;

import java.util.Arrays;
import java.util.List;

public class RsCommenter implements Commenter, CodeDocumentationAwareCommenter, SelfManagingCommenter<CommentHolder> {

    private static final List<String> LINE_PREFIXES = Arrays.asList(
        RsDocKind.OuterEol.getPrefix(),
        RsDocKind.InnerEol.getPrefix(),
        "//"
    );

    // act like there are no doc comments, these are handled in RsEnterInLineCommentHandler
    @Override
    public boolean isDocumentationComment(@Nullable PsiComment element) {
        return false;
    }

    @Nullable
    @Override
    public IElementType getDocumentationCommentTokenType() {
        return null;
    }

    @Nullable
    @Override
    public String getDocumentationCommentLinePrefix() {
        return null;
    }

    @Nullable
    @Override
    public String getDocumentationCommentPrefix() {
        return null;
    }

    @Nullable
    @Override
    public String getDocumentationCommentSuffix() {
        return null;
    }

    @Nullable
    @Override
    public IElementType getLineCommentTokenType() {
        return null;
    }

    @NotNull
    @Override
    public IElementType getBlockCommentTokenType() {
        return RustParserDefinition.BLOCK_COMMENT;
    }

    @NotNull
    @Override
    public String getLineCommentPrefix() {
        return "//";
    }

    @NotNull
    @Override
    public String getBlockCommentPrefix() {
        return "/*";
    }

    @NotNull
    @Override
    public String getBlockCommentSuffix() {
        return "*/";
    }

    // unused because we implement SelfManagingCommenter
    @NotNull
    @Override
    public String getCommentedBlockCommentPrefix() {
        return "*//*";
    }

    @NotNull
    @Override
    public String getCommentedBlockCommentSuffix() {
        return "*//*";
    }

    @NotNull
    @Override
    public String getBlockCommentPrefix(int selectionStart, @NotNull Document document, @NotNull CommentHolder data) {
        return getBlockCommentPrefix();
    }

    @NotNull
    @Override
    public String getBlockCommentSuffix(int selectionEnd, @NotNull Document document, @NotNull CommentHolder data) {
        return getBlockCommentSuffix();
    }

    @Nullable
    @Override
    public TextRange getBlockCommentRange(int selectionStart, int selectionEnd, @NotNull Document document, @NotNull CommentHolder data) {
        return SelfManagingCommenterUtil.getBlockCommentRange(
            selectionStart,
            selectionEnd,
            document,
            getBlockCommentPrefix(),
            getBlockCommentSuffix()
        );
    }

    @NotNull
    @Override
    public TextRange insertBlockComment(int startOffset, int endOffset, @NotNull Document document, @Nullable CommentHolder data) {
        return SelfManagingCommenterUtil.insertBlockComment(
            startOffset,
            endOffset,
            document,
            getBlockCommentPrefix(),
            getBlockCommentSuffix()
        );
    }

    @Override
    public void uncommentBlockComment(int startOffset, int endOffset, @NotNull Document document, @Nullable CommentHolder data) {
        SelfManagingCommenterUtil.uncommentBlockComment(
            startOffset,
            endOffset,
            document,
            getBlockCommentPrefix(),
            getBlockCommentSuffix()
        );
    }

    @Override
    public boolean isLineCommented(int line, int offset, @NotNull Document document, @NotNull CommentHolder data) {
        CharSequence chars = document.getCharsSequence();
        for (String prefix : LINE_PREFIXES) {
            if (CharArrayUtil.regionMatches(chars, offset, prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void commentLine(int line, int offset, @NotNull Document document, @NotNull CommentHolder data) {
        boolean addSpace = data.useSpaceAfterLineComment();
        document.insertString(offset, "//" + (addSpace ? " " : ""));
    }

    @Override
    public void uncommentLine(int line, int offset, @NotNull Document document, @NotNull CommentHolder data) {
        CharSequence chars = document.getCharsSequence();
        int prefixLen = -1;
        for (String prefix : LINE_PREFIXES) {
            if (CharArrayUtil.regionMatches(chars, offset, prefix)) {
                prefixLen = prefix.length();
                break;
            }
        }
        if (prefixLen < 0) return;
        boolean hasSpace = data.useSpaceAfterLineComment() &&
            CharArrayUtil.regionMatches(chars, offset + prefixLen, " ");
        document.deleteString(offset, offset + prefixLen + (hasSpace ? 1 : 0));
    }

    @NotNull
    @Override
    public String getCommentPrefix(int line, @NotNull Document document, @NotNull CommentHolder data) {
        return getLineCommentPrefix();
    }

    @NotNull
    @Override
    public CommentHolder createBlockCommentingState(int selectionStart, int selectionEnd, @NotNull Document document, @NotNull PsiFile file) {
        return new CommentHolder(file);
    }

    @NotNull
    @Override
    public CommentHolder createLineCommentingState(int startLine, int endLine, @NotNull Document document, @NotNull PsiFile file) {
        return new CommentHolder(file);
    }
}
