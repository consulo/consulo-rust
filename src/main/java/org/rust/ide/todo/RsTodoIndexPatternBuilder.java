/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.todo;

import com.intellij.lexer.Lexer;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.search.IndexPatternBuilder;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.rust.lang.core.lexer.RsLexer;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.RsTokenType;

import static org.rust.lang.core.parser.RustParserDefinition.INNER_EOL_DOC_COMMENT;
import static org.rust.lang.core.parser.RustParserDefinition.OUTER_EOL_DOC_COMMENT;

public class RsTodoIndexPatternBuilder implements IndexPatternBuilder {

    @Override
    public Lexer getIndexingLexer(PsiFile file) {
        return file instanceof RsFile ? new RsLexer() : null;
    }

    @Override
    public TokenSet getCommentTokenSet(PsiFile file) {
        return file instanceof RsFile ? org.rust.lang.core.psi.RsTokenType.RS_COMMENTS : null;
    }

    @Override
    public int getCommentStartDelta(IElementType tokenType) {
        if (tokenType != null && RsTokenType.RS_REGULAR_COMMENTS.contains(tokenType)) {
            return 2;
        }
        if (tokenType != null && RsTokenType.RS_DOC_COMMENTS.contains(tokenType)) {
            return 3;
        }
        return 0;
    }

    @Override
    public int getCommentEndDelta(IElementType tokenType) {
        return tokenType != null && RsTokenType.RS_BLOCK_COMMENTS.contains(tokenType) ? 2 : 0;
    }

    @Override
    public String getCharsAllowedInContinuationPrefix(IElementType tokenType) {
        if (tokenType == INNER_EOL_DOC_COMMENT) {
            return "/!";
        }
        if (tokenType == OUTER_EOL_DOC_COMMENT) {
            return "/";
        }
        if (RsTokenType.RS_BLOCK_COMMENTS.contains(tokenType)) {
            return "*";
        }
        return "";
    }
}
