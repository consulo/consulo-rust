/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsPatField;
import org.rust.lang.core.psi.RsPatFieldFull;

public final class RsPatFieldUtil {
    private RsPatFieldUtil() {
    }

    @NotNull
    public static RsPatFieldKind getKind(@NotNull RsPatField field) {
        if (field.getPatBinding() != null) {
            return new RsPatFieldKind.Shorthand(field.getPatBinding(), field.getBox() != null);
        }
        RsPatFieldFull full = field.getPatFieldFull();
        assert full != null;
        return new RsPatFieldKind.Full(full.getReferenceNameElement(), full.getPat());
    }

    @NotNull
    public static String getFieldName(@NotNull RsPatFieldKind kind) {
        if (kind instanceof RsPatFieldKind.Full) {
            return ((RsPatFieldKind.Full) kind).getFieldName();
        }
        return ((RsPatFieldKind.Shorthand) kind).getFieldName();
    }
}
