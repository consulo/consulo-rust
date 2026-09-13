/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsPatField;
import org.rust.lang.core.psi.RsPatFieldFull;
import org.rust.lang.core.psi.ext.*;

public final class RsPatFieldUtil {
    private RsPatFieldUtil() {
    }

    @Nonnull
    public static RsPatFieldKind getKind(@Nonnull RsPatField field) {
        if (field.getPatBinding() != null) {
            return new RsPatFieldKind.Shorthand(field.getPatBinding(), field.getBox() != null);
        }
        RsPatFieldFull full = field.getPatFieldFull();
        assert full != null;
        return new RsPatFieldKind.Full(full.getReferenceNameElement(), full.getPat());
    }

    @Nonnull
    public static String getFieldName(@Nonnull RsPatFieldKind kind) {
        if (kind instanceof RsPatFieldKind.Full) {
            return ((RsPatFieldKind.Full) kind).getFieldName();
        }
        return ((RsPatFieldKind.Shorthand) kind).getFieldName();
    }
}
