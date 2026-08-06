/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty;

import java.util.Collections;

public class TyUnit extends TyTuple {
    public static final TyUnit INSTANCE = new TyUnit();

    private TyUnit() {
        super(Collections.emptyList());
    }
}
