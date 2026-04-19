/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import org.jetbrains.annotations.NotNull;

/**
 * Lightweight representation of proc macro call items for hash calculation.
 */
public class ProcMacroCallLight {
    @NotNull
    private final String body;

    public ProcMacroCallLight(@NotNull String body) {
        this.body = body;
    }

    @NotNull
    public String getBody() {
        return body;
    }
}
