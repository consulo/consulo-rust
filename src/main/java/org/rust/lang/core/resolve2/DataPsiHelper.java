/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.RsItemsOwner;
import org.rust.lang.core.psi.ext.RsMod;

public interface DataPsiHelper {
    @Nullable
    ModData psiToData(@NotNull RsItemsOwner scope);

    @Nullable
    RsMod dataToPsi(@NotNull ModData data);

    @Nullable
    default ModData findModData(@NotNull ModPath path) {
        return null;
    }
}
