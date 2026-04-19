/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions.macroExpansion;

/**
 * Action for showing first-level expansion of ordinary macros.
 */
public class RsShowSingleStepMacroExpansionAction extends RsShowMacroExpansionActions.RsShowMacroExpansionActionBase {
    public RsShowSingleStepMacroExpansionAction() {
        super(false);
    }
}
