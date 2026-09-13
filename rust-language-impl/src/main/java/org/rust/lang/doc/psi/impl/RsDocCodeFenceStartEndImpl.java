/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi.impl;

import consulo.language.ast.IElementType;
import jakarta.annotation.Nonnull;
import org.rust.lang.doc.psi.RsDocCodeFenceStartEnd;

public class RsDocCodeFenceStartEndImpl extends RsDocElementImpl implements RsDocCodeFenceStartEnd {

    public RsDocCodeFenceStartEndImpl(@Nonnull IElementType type) {
        super(type);
    }
}
