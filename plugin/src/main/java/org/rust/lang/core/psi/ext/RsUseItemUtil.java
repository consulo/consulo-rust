/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsUseItem;
import org.rust.lang.core.stubs.RsUseItemStub;

public final class RsUseItemUtil {
    private RsUseItemUtil() {
    }

    @Nonnull
    public static final StubbedAttributeProperty<RsUseItem, RsUseItemStub> HAS_PRELUDE_IMPORT_PROP =
        new StubbedAttributeProperty<>(
            it -> it.hasAttribute("prelude_import"),
            RsUseItemStub::getMayHavePreludeImport
        );

    public static boolean isReexport(@Nonnull RsUseItem useItem) {
        return useItem.getVisibility() != RsVisibility.Private.INSTANCE;
    }
}
