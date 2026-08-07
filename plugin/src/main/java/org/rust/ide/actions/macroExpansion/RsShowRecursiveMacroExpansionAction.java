/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions.macroExpansion;

import consulo.annotation.component.ActionImpl;

/**
 * Action for showing recursive expansion of ordinary macros.
 */
@ActionImpl(id = "Rust.ShowRecursiveMacroExpansionAction")
public class RsShowRecursiveMacroExpansionAction extends RsShowMacroExpansionActions.RsShowMacroExpansionActionBase {
    public RsShowRecursiveMacroExpansionAction() {
        super(true);
    }
}
