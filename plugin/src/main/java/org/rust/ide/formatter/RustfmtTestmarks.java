/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.formatter;

import org.rust.openapiext.Testmark;

public final class RustfmtTestmarks {
    public static final RustfmtTestmarks INSTANCE = new RustfmtTestmarks();

    private RustfmtTestmarks() {
    }

    public static final Testmark RustfmtUsed = new Testmark() {};
}
