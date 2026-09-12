/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.todo;

import consulo.language.lexer.LexerPosition;
import consulo.language.psi.stub.BaseFilterLexer;
import consulo.language.psi.stub.OccurrenceConsumer;
import consulo.language.psi.search.UsageSearchContext;
import consulo.language.ast.IElementType;
import org.rust.lang.core.lexer.RsLexer;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsTokenSets;

class RsFilterLexer extends BaseFilterLexer {

    RsFilterLexer(OccurrenceConsumer consumer) {
        super(new RsLexer(), consumer);
    }

    @Override
    public void advance() {
        IElementType tokenType = myDelegate.getTokenType();
        if (tokenType != null && RsTokenSets.RS_COMMENTS.contains(tokenType)) {
            scanWordsInToken(UsageSearchContext.IN_COMMENTS, false, false);
            advanceTodoItemCountsInToken();
        } else if (tokenType == RsElementTypes.IDENTIFIER) {
            addOccurrenceInToken(UsageSearchContext.IN_CODE);
            if ("todo".equals(myDelegate.getTokenText())) {
                if (nextToken() == RsElementTypes.EXCL) {
                    advanceTodoItemCountsInToken();
                }
            }
        }

        myDelegate.advance();
    }

    private IElementType nextToken() {
        LexerPosition position = myDelegate.getCurrentPosition();
        try {
            myDelegate.advance();
            return myDelegate.getTokenType();
        } finally {
            myDelegate.restore(position);
        }
    }
}
