/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions;

import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import consulo.localize.LocalizeValue;

public class RsShowRecursiveMacroExpansionIntention extends RsShowMacroExpansionIntentionBase {
    @Override
    protected boolean getExpandRecursively() {
        return true;
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.show.recursive.macro.expansion"));
        }
}
