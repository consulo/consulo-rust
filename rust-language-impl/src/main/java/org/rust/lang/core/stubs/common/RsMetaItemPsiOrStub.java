/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs.common;

import jakarta.annotation.Nullable;

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
     * The attribute's name, when its path is a single identifier - {@code derive} in
     * {@code #[derive(Clone)]}. A qualified path such as {@code #[foo::bar]} has no simple name.
     */
    @Nullable
    default String getName() {
        RsPathPsiOrStub path = getPath();
        if (path == null || path.getHasColonColon()) return null;
        return path.getReferenceName();
    }
}
