/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.cargo.api.util.AutoInjectedCrates;

public interface OverloadableBinaryOperator {
    @Nonnull String getTraitName();
    @Nonnull String getItemName();
    @Nonnull String getFnName();
    @Nonnull String getSign();

    @Nullable
    default RsTraitItem findTrait(@Nonnull KnownItems items) {
        return items.findLangItem(getItemName(), AutoInjectedCrates.CORE, RsTraitItem.class);
    }
}
