/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import jakarta.annotation.Nonnull;

/**
 * Lightweight representation of proc macro call items for hash calculation.
 */
public class ProcMacroCallLight {
    @Nonnull
    private final String body;

    public ProcMacroCallLight(@Nonnull String body) {
        this.body = body;
    }

    @Nonnull
    public String getBody() {
        return body;
    }
}
