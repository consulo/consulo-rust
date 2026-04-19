/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.lexer;

import com.intellij.lexer.LayeredLexer;
import com.intellij.psi.tree.IElementType;

public class RsHighlightingLexer extends LayeredLexer {
    public RsHighlightingLexer() {
        super(new RsLexer());
        for (IElementType type : RsEscapesLexer.ESCAPABLE_LITERALS_TOKEN_SET.getTypes()) {
            registerLayer(RsEscapesLexer.of(type), type);
        }
    }
}
