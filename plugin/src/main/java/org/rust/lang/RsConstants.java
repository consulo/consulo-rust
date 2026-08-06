/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang;

public final class RsConstants {
    public static final RsConstants INSTANCE = new RsConstants();

    public static final String MOD_RS_FILE = "mod.rs";
    public static final String MAIN_RS_FILE = "main.rs";
    public static final String LIB_RS_FILE = "lib.rs";
    public static final String ERROR_INDEX_URL = "https://doc.rust-lang.org/error-index.html";

    private RsConstants() {
    }
}
