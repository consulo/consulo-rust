/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi.ext;

import org.jetbrains.annotations.NotNull;

/**
 * Delegates to {@link RsDocIElementTypeExt} which contains the full implementation.
 */
public final class IElementType {
    private IElementType() {
    }

    /**
     * Checks if the given element type is a doc comment leaf token (DOC_GAP or DOC_DATA).
     * @see RsDocIElementTypeExt#isDocCommentLeafToken(com.intellij.psi.tree.IElementType)
     */
    public static boolean isDocCommentLeafToken(@NotNull com.intellij.psi.tree.IElementType elementType) {
        return RsDocIElementTypeExt.isDocCommentLeafToken(elementType);
    }
}
