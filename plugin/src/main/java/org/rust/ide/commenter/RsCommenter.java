/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.commenter;

import consulo.language.SelfManagingCommenter;
import com.intellij.codeInsight.generation.SelfManagingCommenterUtil;
import consulo.language.CodeDocumentationAwareCommenter;
import consulo.language.Commenter;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiFile;
import consulo.language.ast.IElementType;
import consulo.util.lang.CharArrayUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.parser.RustParserDefinition;
import org.rust.lang.doc.psi.RsDocKind;

import java.util.Arrays;
import java.util.List;

public class RsCommenter implements Commenter, CodeDocumentationAwareCommenter, SelfManagingCommenter<CommentHolder> {
    @jakarta.annotation.Nonnull @Override public consulo.language.Language getLanguage() { return org.rust.lang.RsLanguage.INSTANCE; }


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

    @Nonnull
    @Override
    public IElementType getBlockCommentTokenType() {
        return RustParserDefinition.BLOCK_COMMENT;
    }

    @Nonnull
    @Override
    public String getLineCommentPrefix() {
        return "//";
    }

    @Nonnull
    @Override
    public String getBlockCommentPrefix() {
        return "/*";
    }

    @Nonnull
    @Override
    public String getBlockCommentSuffix() {
        return "*/";
    }

    // unused because we implement SelfManagingCommenter
    @Nonnull
    @Override
    public String getCommentedBlockCommentPrefix() {
        return "*//*";
    }

    @Nonnull
    @Override
    public String getCommentedBlockCommentSuffix() {
        return "*//*";
    }

    @Nonnull
    @Override
    public String getBlockCommentPrefix(int selectionStart, @Nonnull Document document, @Nonnull CommentHolder data) {
        return getBlockCommentPrefix();
    }

    @Nonnull
    @Override
    public String getBlockCommentSuffix(int selectionEnd, @Nonnull Document document, @Nonnull CommentHolder data) {
        return getBlockCommentSuffix();
    }

    @Nullable
    @Override
    public TextRange getBlockCommentRange(int selectionStart, int selectionEnd, @Nonnull Document document, @Nonnull CommentHolder data) {
        return SelfManagingCommenterUtil.getBlockCommentRange(
            selectionStart,
            selectionEnd,
            document,
            getBlockCommentPrefix(),
            getBlockCommentSuffix()
        );
    }

    @Nonnull
    @Override
    public TextRange insertBlockComment(int startOffset, int endOffset, @Nonnull Document document, @Nullable CommentHolder data) {
        String prefix = getBlockCommentPrefix();
        String suffix = getBlockCommentSuffix();
        document.insertString(endOffset, suffix);
        document.insertString(startOffset, prefix);
        return new TextRange(startOffset, endOffset + prefix.length() + suffix.length());
    }

    @Override
    public void uncommentBlockComment(int startOffset, int endOffset, @Nonnull Document document, @Nullable CommentHolder data) {
        SelfManagingCommenterUtil.uncommentBlockComment(
            startOffset,
            endOffset,
            document,
            getBlockCommentPrefix(),
            getBlockCommentSuffix()
        );
    }

    @Override
    public boolean isLineCommented(int line, int offset, @Nonnull Document document, @Nonnull CommentHolder data) {
        CharSequence chars = document.getCharsSequence();
        for (String prefix : LINE_PREFIXES) {
            if (CharArrayUtil.regionMatches(chars, offset, prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void commentLine(int line, int offset, @Nonnull Document document, @Nonnull CommentHolder data) {
        boolean addSpace = data.useSpaceAfterLineComment();
        document.insertString(offset, "//" + (addSpace ? " " : ""));
    }

    @Override
    public void uncommentLine(int line, int offset, @Nonnull Document document, @Nonnull CommentHolder data) {
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

    @Nonnull
    @Override
    public String getCommentPrefix(int line, @Nonnull Document document, @Nonnull CommentHolder data) {
        return getLineCommentPrefix();
    }

    @Nonnull
    @Override
    public CommentHolder createBlockCommentingState(int selectionStart, int selectionEnd, @Nonnull Document document, @Nonnull PsiFile file) {
        return new CommentHolder(file);
    }

    @Nonnull
    @Override
    public CommentHolder createLineCommentingState(int startLine, int endLine, @Nonnull Document document, @Nonnull PsiFile file) {
        return new CommentHolder(file);
    }
}
