/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.regions;

/**
 * Static data that has an "infinite" lifetime. Top in the region lattice.
 */
public class ReStatic extends Region {
    public static final ReStatic INSTANCE = new ReStatic();

    private ReStatic() {
    }

    @Override
    public String toString() {
        return "'static";
    }
}
