/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty;

import jakarta.annotation.Nonnull;

public class TyChar extends TyPrimitive {
    public static final TyChar INSTANCE = new TyChar();
    private TyChar() {}

    @Nonnull
    @Override
    public String getName() { return "char"; }
}
