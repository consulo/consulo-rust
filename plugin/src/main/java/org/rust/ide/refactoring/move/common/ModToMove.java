/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.move.common;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsMod;

public class ModToMove extends ElementToMove {

    @Nonnull
    private final RsMod mod;

    public ModToMove(@Nonnull RsMod mod) {
        this.mod = mod;
    }

    @Nonnull
    public RsMod getMod() {
        return mod;
    }

    @Override
    @Nonnull
    public RsElement getElement() {
        return mod;
    }
}
