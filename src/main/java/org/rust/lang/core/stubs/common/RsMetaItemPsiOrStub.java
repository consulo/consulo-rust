/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs.common;

import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public interface RsMetaItemPsiOrStub {
    @Nullable
    RsPathPsiOrStub getPath();

    @Nullable
    RsMetaItemArgsPsiOrStub getMetaItemArgs();

    default List<? extends RsMetaItemPsiOrStub> getMetaItemArgsList() {
        RsMetaItemArgsPsiOrStub args = getMetaItemArgs();
        return args != null ? args.getMetaItemList() : Collections.emptyList();
    }

    boolean getHasEq();

    @Nullable
    String getValue();

    /**
     * Returns the name of this meta item, derived from its path's reference name.
     */
    @Nullable
    default String getName() {
        RsPathPsiOrStub path = getPath();
        return path != null ? path.getReferenceName() : null;
    }
}
