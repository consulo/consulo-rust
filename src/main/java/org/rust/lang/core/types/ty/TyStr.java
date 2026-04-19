/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty;

import org.jetbrains.annotations.NotNull;

public class TyStr extends TyPrimitive {
    public static final TyStr INSTANCE = new TyStr();
    private TyStr() {}

    @NotNull
    @Override
    public String getName() { return "str"; }
}
