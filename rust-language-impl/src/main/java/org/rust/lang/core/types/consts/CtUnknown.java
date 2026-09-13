/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.consts;

public class CtUnknown extends Const {
    public static final CtUnknown INSTANCE = new CtUnknown();

    private CtUnknown() {
        super();
    }

    @Override
    public String toString() {
        return "<unknown>";
    }
}
