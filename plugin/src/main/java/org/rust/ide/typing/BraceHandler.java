/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.typing;

import consulo.codeEditor.Editor;

public interface BraceHandler {
    BraceKind getOpening();
    BraceKind getClosing();
    boolean shouldComplete(Editor editor);
    int calculateBalance(Editor editor);
}
