/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions.macroExpansion;

import org.rust.lang.core.macros.MacroExpansion;
import org.rust.lang.core.psi.ext.RsPossibleMacroCall;

/** Data class to group title and expansions of macro to show them in the view. */
public record MacroExpansionViewDetails(
    RsPossibleMacroCall macroToExpand,
    String title,
    MacroExpansion expansion
) {}
