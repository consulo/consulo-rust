/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions.util.macros;

/**
 * See {@link org.rust.ide.intentions.RsElementBaseIntentionAction#getAttributeMacroHandlingStrategy()} and
 * {@link org.rust.ide.intentions.RsElementBaseIntentionAction#getFunctionLikeMacroHandlingStrategy()}
 */
public enum InvokeInside {
    MACRO_CALL,
    MACRO_EXPANSION
}
