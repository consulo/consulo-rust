/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.regions;

public class ReUnknown extends Region {
    public static final ReUnknown INSTANCE = new ReUnknown();

    private ReUnknown() {
    }

    @Override
    public String toString() {
        return "'_";
    }
}
