/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.move.common;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsItemElement;

public class ItemToMove extends ElementToMove {

    @Nonnull
    private final RsItemElement item;

    public ItemToMove(@Nonnull RsItemElement item) {
        this.item = item;
    }

    @Nonnull
    public RsItemElement getItem() {
        return item;
    }

    @Override
    @Nonnull
    public RsElement getElement() {
        return item;
    }
}
