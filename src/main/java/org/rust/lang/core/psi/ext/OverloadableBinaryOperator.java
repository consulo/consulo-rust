/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.cargo.util.AutoInjectedCrates;

public interface OverloadableBinaryOperator {
    @NotNull String getTraitName();
    @NotNull String getItemName();
    @NotNull String getFnName();
    @NotNull String getSign();

    @Nullable
    default RsTraitItem findTrait(@NotNull KnownItems items) {
        return items.findLangItem(getItemName(), AutoInjectedCrates.CORE, RsTraitItem.class);
    }
}
