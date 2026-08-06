/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions.macroExpansion;

/**
 * Action for showing recursive expansion of ordinary macros.
 */
public class RsShowRecursiveMacroExpansionAction extends RsShowMacroExpansionActions.RsShowMacroExpansionActionBase {
    public RsShowRecursiveMacroExpansionAction() {
        super(true);
    }
}
