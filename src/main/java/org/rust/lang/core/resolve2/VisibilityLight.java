/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataOutput;
import java.io.IOException;

/**
 * Lightweight representation of visibility, used during mod collection and hash calculation.
 */
public class VisibilityLight {

    public static final VisibilityLight PUB = new VisibilityLight(Kind.PUB, null);
    public static final VisibilityLight PRIV = new VisibilityLight(Kind.PRIV, null);
    public static final VisibilityLight PUB_CRATE = new VisibilityLight(Kind.PUB_CRATE, null);
    public static final VisibilityLight CFG_DISABLED = new VisibilityLight(Kind.CFG_DISABLED, null);

    public static final VisibilityLight Public = PUB;
    public static final VisibilityLight Private = PRIV;
    public static final VisibilityLight CfgDisabled = CFG_DISABLED;

    public enum Kind {
        PUB,
        PRIV,
        PUB_CRATE,
        RESTRICTED,
        CFG_DISABLED
    }

    @NotNull
    private final Kind kind;
    @Nullable
    private final String[] restrictedPath;

    public VisibilityLight(@NotNull Kind kind, @Nullable String[] restrictedPath) {
        this.kind = kind;
        this.restrictedPath = restrictedPath;
    }

    @NotNull
    public Kind getKind() {
        return kind;
    }

    @Nullable
    public String[] getRestrictedPath() {
        return restrictedPath;
    }

    public void writeTo(@NotNull DataOutput data) throws IOException {
        data.writeByte(kind.ordinal());
    }
}
