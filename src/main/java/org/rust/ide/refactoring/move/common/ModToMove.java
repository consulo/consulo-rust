/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.move.common;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsMod;

public class ModToMove extends ElementToMove {

    @NotNull
    private final RsMod mod;

    public ModToMove(@NotNull RsMod mod) {
        this.mod = mod;
    }

    @NotNull
    public RsMod getMod() {
        return mod;
    }

    @Override
    @NotNull
    public RsElement getElement() {
        return mod;
    }
}
