/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import jakarta.annotation.Nonnull;

public interface MacroCallInfoBase {
    @Nonnull
    ModData getContainingMod();
}
