/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi.ext;

import consulo.language.ast.IElementType;
import jakarta.annotation.Nonnull;
import org.rust.lang.doc.psi.RsDocElementTypes;

public final class RsDocIElementTypeExt {

    private RsDocIElementTypeExt() {
    }

    public static boolean isDocCommentLeafToken(@Nonnull IElementType type) {
        return type == RsDocElementTypes.DOC_GAP || type == RsDocElementTypes.DOC_DATA;
    }
}
