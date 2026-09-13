/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.ext.RsItemsOwner;
import org.rust.lang.core.psi.ext.RsMod;

public interface DataPsiHelper {
    @Nullable
    ModData psiToData(@Nonnull RsItemsOwner scope);

    @Nullable
    RsMod dataToPsi(@Nonnull ModData data);

    @Nullable
    default ModData findModData(@Nonnull ModPath path) {
        return null;
    }
}
