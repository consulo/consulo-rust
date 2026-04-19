/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.move.common;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsItemElement;

public class ItemToMove extends ElementToMove {

    @NotNull
    private final RsItemElement item;

    public ItemToMove(@NotNull RsItemElement item) {
        this.item = item;
    }

    @NotNull
    public RsItemElement getItem() {
        return item;
    }

    @Override
    @NotNull
    public RsElement getElement() {
        return item;
    }
}
