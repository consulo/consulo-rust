/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi.impl;

import consulo.language.ast.IElementType;
import jakarta.annotation.Nonnull;
import org.rust.lang.doc.psi.RsDocAutoLink;

public class RsDocAutoLinkImpl extends RsDocElementImpl implements RsDocAutoLink {

    public RsDocAutoLinkImpl(@Nonnull IElementType type) {
        super(type);
    }
}
