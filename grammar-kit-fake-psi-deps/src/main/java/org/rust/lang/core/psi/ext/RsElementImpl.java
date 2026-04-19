/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.impl.source.tree.CompositePsiElement;
import com.intellij.psi.tree.IElementType;

/**
 * Beware, <b>FAKE</b>!
 * This is not a real class used across the plugin. This is a fake class used as an input for Grammar-Kit.
 * Please, read {@code README.md} of this module for more info.
 */
public abstract class RsElementImpl extends CompositePsiElement {
    public RsElementImpl(IElementType type) {
        super(type);
    }
}
