/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.lexer;

import consulo.language.lexer.LayeredLexer;
import consulo.language.ast.IElementType;

public class RsHighlightingLexer extends LayeredLexer {
    public RsHighlightingLexer() {
        super(new RsLexer());
        for (IElementType type : RsEscapesLexer.ESCAPABLE_LITERALS_TOKEN_SET.getTypes()) {
            registerLayer(RsEscapesLexer.of(type), type);
        }
    }
}
