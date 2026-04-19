/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;

public class RsShowRecursiveMacroExpansionIntention extends RsShowMacroExpansionIntentionBase {
    @Override
    protected boolean getExpandRecursively() {
        return true;
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.show.recursive.macro.expansion");
    }
}
