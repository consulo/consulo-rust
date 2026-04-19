/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty;

import org.jetbrains.annotations.NotNull;

public class TyBool extends TyPrimitive {
    public static final TyBool INSTANCE = new TyBool();
    private TyBool() {}

    @NotNull
    @Override
    public String getName() { return "bool"; }
}
