/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.HighlighterIterator;

public final class RsBraceHandlersUtil {

    private RsBraceHandlersUtil() {
    }

    public static HighlighterIterator createLexer(Editor editor, int offset) {
        if (!RsTypingUtils.isValidOffset(offset, editor.getDocument().getCharsSequence())) return null;
        HighlighterIterator lexer = ((EditorEx) editor).getHighlighter().createIterator(offset);
        if (lexer.atEnd()) return null;
        return lexer;
    }
}
