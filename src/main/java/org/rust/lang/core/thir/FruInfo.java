/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.thir;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.types.ty.Ty;

import java.util.List;

/** {@code Foo { ..base }} */
public class FruInfo {
    @NotNull
    public final ThirExpr base;
    @NotNull
    public final List<Ty> fieldTypes;

    public FruInfo(@NotNull ThirExpr base, @NotNull List<Ty> fieldTypes) {
        this.base = base;
        this.fieldTypes = fieldTypes;
    }
}
