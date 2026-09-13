/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.todo;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.lexer.Lexer;
import consulo.language.psi.PsiFile;
import consulo.language.psi.search.IndexPatternBuilder;
import consulo.language.ast.IElementType;
import consulo.language.ast.TokenSet;
import org.rust.lang.core.lexer.RsLexer;
import org.rust.lang.core.psi.impl.RsFile;

import static org.rust.lang.core.psi.RsTokenType.INNER_EOL_DOC_COMMENT;
import static org.rust.lang.core.psi.RsTokenType.OUTER_EOL_DOC_COMMENT;
import org.rust.lang.core.psi.impl.RsTokenSets;
import org.rust.lang.core.psi.RsTokenType;

@ExtensionImpl
public class RsTodoIndexPatternBuilder implements IndexPatternBuilder {

    @Override
    public Lexer getIndexingLexer(PsiFile file) {
        return file instanceof RsFile ? new RsLexer() : null;
    }

    @Override
    public TokenSet getCommentTokenSet(PsiFile file) {
        return file instanceof RsFile ? RsTokenSets.RS_COMMENTS : null;
    }

    @Override
    public int getCommentStartDelta(IElementType tokenType) {
        if (tokenType != null && RsTokenSets.RS_REGULAR_COMMENTS.contains(tokenType)) {
            return 2;
        }
        if (tokenType != null && RsTokenSets.RS_DOC_COMMENTS.contains(tokenType)) {
            return 3;
        }
        return 0;
    }

    @Override
    public int getCommentEndDelta(IElementType tokenType) {
        return tokenType != null && RsTokenSets.RS_BLOCK_COMMENTS.contains(tokenType) ? 2 : 0;
    }

    @Override
    public String getCharsAllowedInContinuationPrefix(IElementType tokenType) {
        if (tokenType == INNER_EOL_DOC_COMMENT) {
            return "/!";
        }
        if (tokenType == OUTER_EOL_DOC_COMMENT) {
            return "/";
        }
        if (RsTokenSets.RS_BLOCK_COMMENTS.contains(tokenType)) {
            return "*";
        }
        return "";
    }
}
