/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.search;

import consulo.language.cacheBuilder.DefaultWordsScanner;
import consulo.language.ast.TokenSet;
import org.rust.lang.core.lexer.RsLexer;
import org.rust.lang.core.parser.RustParserDefinition;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.impl.RsTokenSets;

public class RsWordScanner extends DefaultWordsScanner {

    private static final int VERSION = 1;

    public RsWordScanner() {
        super(
            new RsLexer(),
            TokenSet.create(RsElementTypes.IDENTIFIER),
            RsTokenSets.RS_COMMENTS,
            RsTokenSets.RS_ALL_STRING_LITERALS
        );
        // This actually means that it's possible to do language injections into Rust string literals
        setMayHaveFileRefsInLiterals(true);
    }

    public int getVersion() {
        return RustParserDefinition.LEXER_VERSION + VERSION;
    }
}
