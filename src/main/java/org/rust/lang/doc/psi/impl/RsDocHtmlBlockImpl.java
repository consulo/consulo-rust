/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi.impl;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.doc.psi.RsDocHtmlBlock;

public class RsDocHtmlBlockImpl extends RsDocElementImpl implements RsDocHtmlBlock {

    public RsDocHtmlBlockImpl(@NotNull IElementType type) {
        super(type);
    }
}
