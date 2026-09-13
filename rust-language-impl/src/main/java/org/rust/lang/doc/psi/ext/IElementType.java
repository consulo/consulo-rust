/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi.ext;

import jakarta.annotation.Nonnull;

/**
 * Delegates to {@link RsDocIElementTypeExt} which contains the full implementation.
 */
public final class IElementType {
    private IElementType() {
    }

    /**
     * Checks if the given element type is a doc comment leaf token (DOC_GAP or DOC_DATA).
     * @see RsDocIElementTypeExt#isDocCommentLeafToken(consulo.language.ast.IElementType)
     */
    public static boolean isDocCommentLeafToken(@Nonnull consulo.language.ast.IElementType elementType) {
        return RsDocIElementTypeExt.isDocCommentLeafToken(elementType);
    }
}
