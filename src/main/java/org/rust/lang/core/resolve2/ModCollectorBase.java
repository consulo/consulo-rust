/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.ext.RsElement;

/**
 * This class is used:
 * - When collecting explicit items: filling ModData + calculating hash
 * - When collecting expanded items: filling ModData
 * - When checking if file was changed: calculating hash
 */
public final class ModCollectorBase {

    private ModCollectorBase() {}

    public static void collectMod(
        @NotNull StubElement<? extends RsElement> itemsOwner,
        boolean isDeeplyEnabledByCfg,
        @NotNull ModVisitor visitor,
        @NotNull Crate crate
    ) {
        // Simplified placeholder for the actual collection logic
        // In actual implementation, iterates over child stubs and calls visitor methods
        visitor.afterCollectMod();
    }
}
