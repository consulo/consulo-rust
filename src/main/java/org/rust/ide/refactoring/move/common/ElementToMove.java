/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.move.common;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsModItem;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsItemElement;
import org.rust.lang.core.psi.ext.RsMod;

public abstract class ElementToMove {

    @NotNull
    public abstract RsElement getElement();

    @NotNull
    public static ElementToMove fromItem(@NotNull RsItemElement item) {
        if (item instanceof RsModItem) {
            return new ModToMove((RsMod) item);
        }
        return new ItemToMove(item);
    }
}
