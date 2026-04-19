/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import org.jetbrains.annotations.NotNull;

/**
 * Lightweight representation of macro 2.0 definition items.
 */
public class Macro2DefLight {
    @NotNull
    private final String name;
    @NotNull
    private final String body;
    @NotNull
    private final String bodyHash;
    private final boolean hasRustcBuiltinMacro;
    @NotNull
    private final VisibilityLight visibility;
    private final boolean isDeeplyEnabledByCfg;

    public Macro2DefLight(
        @NotNull String name,
        @NotNull String body,
        @NotNull String bodyHash,
        boolean hasRustcBuiltinMacro,
        @NotNull VisibilityLight visibility,
        boolean isDeeplyEnabledByCfg
    ) {
        this.name = name;
        this.body = body;
        this.bodyHash = bodyHash;
        this.hasRustcBuiltinMacro = hasRustcBuiltinMacro;
        this.visibility = visibility;
        this.isDeeplyEnabledByCfg = isDeeplyEnabledByCfg;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public String getBody() {
        return body;
    }

    @NotNull
    public String getBodyHash() {
        return bodyHash;
    }

    public boolean isHasRustcBuiltinMacro() {
        return hasRustcBuiltinMacro;
    }

    @NotNull
    public VisibilityLight getVisibility() {
        return visibility;
    }

    public boolean isDeeplyEnabledByCfg() {
        return isDeeplyEnabledByCfg;
    }
}
