/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.thir;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.types.ty.Ty;

import java.util.List;

/** {@code Foo { ..base }} */
public class FruInfo {
    @Nonnull
    public final ThirExpr base;
    @Nonnull
    public final List<Ty> fieldTypes;

    public FruInfo(@Nonnull ThirExpr base, @Nonnull List<Ty> fieldTypes) {
        this.base = base;
        this.fieldTypes = fieldTypes;
    }
}
