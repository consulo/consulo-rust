/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions.util.macros;

public class IntentionInsideMacroExpansionEditorUnsupportedOperationException extends UnsupportedOperationException {
    public IntentionInsideMacroExpansionEditorUnsupportedOperationException() {
        super("It's unexpected to invoke this method on macro expansion fake editor");
    }
}
