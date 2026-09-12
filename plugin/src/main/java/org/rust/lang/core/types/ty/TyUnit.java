/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty;

public class TyUnit extends TyPrimitive {
    public static final TyUnit INSTANCE = new TyUnit();

    private TyUnit() {
    }

    @Override
    public String getName() {
        return "unit";
    }
}
