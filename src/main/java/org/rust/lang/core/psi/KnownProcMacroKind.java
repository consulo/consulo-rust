/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

public enum KnownProcMacroKind {
    IDENTITY,
    ASYNC_MAIN,
    ASYNC_TEST,
    ASYNC_BENCH,
    ASYNC_TRAIT,
    CUSTOM_MAIN,
    CUSTOM_TEST,
    CUSTOM_TEST_RENAME,
    DEFAULT_PURE,
    TEST_WRAPPER;

    public boolean getTreatAsBuiltinAttr() {
        return this != DEFAULT_PURE;
    }
}
