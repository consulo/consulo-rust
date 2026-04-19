/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.search;

import com.intellij.lang.cacheBuilder.DefaultWordsScanner;
import com.intellij.psi.tree.TokenSet;
import org.rust.lang.core.lexer.RsLexer;
import org.rust.lang.core.parser.RustParserDefinition;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsTokenType;

public class RsWordScanner extends DefaultWordsScanner {

    private static final int VERSION = 1;

    public RsWordScanner() {
        super(
            new RsLexer(),
            TokenSet.create(RsElementTypes.IDENTIFIER),
            RsTokenType.RS_COMMENTS,
            RsTokenType.RS_ALL_STRING_LITERALS
        );
        // This actually means that it's possible to do language injections into Rust string literals
        setMayHaveFileRefsInLiterals(true);
    }

    @Override
    public int getVersion() {
        return RustParserDefinition.LEXER_VERSION + VERSION;
    }
}
