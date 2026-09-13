/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import jakarta.annotation.Nonnull;

/**
 * Lightweight representation of macro 2.0 definition items.
 */
public class Macro2DefLight {
    @Nonnull
    private final String name;
    @Nonnull
    private final String body;
    @Nonnull
    private final String bodyHash;
    private final boolean hasRustcBuiltinMacro;
    @Nonnull
    private final VisibilityLight visibility;
    private final boolean isDeeplyEnabledByCfg;

    public Macro2DefLight(
        @Nonnull String name,
        @Nonnull String body,
        @Nonnull String bodyHash,
        boolean hasRustcBuiltinMacro,
        @Nonnull VisibilityLight visibility,
        boolean isDeeplyEnabledByCfg
    ) {
        this.name = name;
        this.body = body;
        this.bodyHash = bodyHash;
        this.hasRustcBuiltinMacro = hasRustcBuiltinMacro;
        this.visibility = visibility;
        this.isDeeplyEnabledByCfg = isDeeplyEnabledByCfg;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    @Nonnull
    public String getBody() {
        return body;
    }

    @Nonnull
    public String getBodyHash() {
        return bodyHash;
    }

    public boolean isHasRustcBuiltinMacro() {
        return hasRustcBuiltinMacro;
    }

    @Nonnull
    public VisibilityLight getVisibility() {
        return visibility;
    }

    public boolean isDeeplyEnabledByCfg() {
        return isDeeplyEnabledByCfg;
    }
}
