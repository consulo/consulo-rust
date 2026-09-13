/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.tt;

/**
 * Specifies whether there is a space <b>after</b> a {@link TokenTree.Leaf.Punct}.
 * The last token is always {@link #Alone}.
 */
public enum Spacing {
    Alone,
    Joint
}
